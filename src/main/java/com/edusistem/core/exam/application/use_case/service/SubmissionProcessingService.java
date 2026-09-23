package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.application.use_case.service.ExamContextLoader.ExamContext;
import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.entity.ExamAnswer;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException;
import com.edusistem.core.exam.domain.inputports.SubmitAnswerSheetUseCase;
import com.edusistem.core.exam.domain.outputports.AnswerSheetProcessorPort;
import com.edusistem.core.exam.domain.outputports.ExamSubmissionRepositoryPort;
import com.edusistem.core.exam.domain.service.BubbleClassifier;
import com.edusistem.core.exam.domain.service.ExamScorer;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.BubbleReading;
import com.edusistem.core.exam.domain.vo.DetectedAnswer;
import com.edusistem.core.exam.domain.vo.QrPayload;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.DomainException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesa la foto de una hoja. Toda la operación (submission + respuestas + resultado) es una única transacción.
 * Los fallos de lectura de imagen NO propagan excepción: la submission queda FAILED con su motivo, para consulta.
 * Los fallos de identificación (QR ilegible/inválido, examen o estudiante incorrectos) sí se rechazan sin persistir.
 */
@Service
public class SubmissionProcessingService implements SubmitAnswerSheetUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmissionProcessingService.class);

    private final ExamContextLoader loader;
    private final AnswerSheetProcessorPort processor;
    private final ExamSubmissionRepositoryPort submissions;
    private final StudentRepositoryPort students;
    private final StudentGroupRepositoryPort studentGroups;
    private final FileStoragePort storage;
    private final BubbleClassifier classifier;
    private final SubmissionDetailsAssembler assembler;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    public SubmissionProcessingService(ExamContextLoader loader, AnswerSheetProcessorPort processor,
                                       ExamSubmissionRepositoryPort submissions, StudentRepositoryPort students,
                                       StudentGroupRepositoryPort studentGroups, FileStoragePort storage,
                                       BubbleClassifier classifier, SubmissionDetailsAssembler assembler,
                                       RecordAuditUseCase audit, Clock clock) {
        this.loader = loader;
        this.processor = processor;
        this.submissions = submissions;
        this.students = students;
        this.studentGroups = studentGroups;
        this.storage = storage;
        this.classifier = classifier;
        this.assembler = assembler;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubmissionDetails submit(SubmissionCommands.Submit command) {
        ExamContext ctx = loader.load(command.teacherId(), command.examId());
        Exam exam = ctx.exam();
        String qrText = null;
        Student student;
        try {
            requireImage(command.image());
            if (!exam.isReady()) {
                throw new BusinessRuleException("EXAM_NOT_READY", "The exam questions are not fully defined");
            }
            loader.requireScale(ctx.evaluation().getTeachingPeriodId());
            qrText = processor.readQrCode(command.image()).orElse(null);
            student = identifyStudent(command, ctx, qrText);
        } catch (DomainException e) {
            audit.failure(command.teacherId(), AuditAction.EXAM_PROCESSED, "Exam", command.examId(),
                    e.getCode() + ": " + e.getMessage());
            throw e;
        }

        Optional<ExamSubmission> existing = submissions.findByExamIdAndStudentId(exam.getId(), student.getId());
        if (existing.isPresent() && !command.replace() && (existing.get().getStatus() == ExamSubmissionStatus.PROCESSED
                || existing.get().getStatus() == ExamSubmissionStatus.REVIEW_REQUIRED)) {
            audit.failure(command.teacherId(), AuditAction.EXAM_PROCESSED, "Exam", command.examId(),
                    "SUBMISSION_ALREADY_EXISTS for student " + student.getStudentCode());
            throw new ConflictException("SUBMISSION_ALREADY_EXISTS",
                    "This student already has a processed submission; use replace=true to process it again");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        ExamSubmission submission = existing.orElseGet(() -> ExamSubmission.builder().examId(exam.getId())
                .studentId(student.getId()).build());
        submission.setStudentCode(student.getStudentCode());
        submission.setDetectedQrData(qrText);
        submission.setImagePath(storage.store("submissions/exam-" + exam.getId(), command.fileName(), command.image()));
        submission.setSubmittedAt(now);
        submission.setStatus(ExamSubmissionStatus.PROCESSING);

        try {
            GradingScale scale = loader.requireScale(ctx.evaluation().getTeachingPeriodId());
            BubbleReading reading = processor.readBubbles(command.image(),
                    new AnswerSheetLayout(exam.getNumberOfQuestions(), exam.optionCount()));
            applyReading(submission, exam, scale, ctx.evaluation().getMaximumScore(), classifier.classify(reading), now);
        } catch (AnswerSheetProcessingException e) {
            submission.markFailed(e.getMessage(), now);
        } catch (RuntimeException e) {
            log.error("Unexpected error processing answer sheet for exam {}", exam.getId(), e);
            submission.markFailed("Unexpected error while processing the image", now);
        }

        ExamSubmission saved = submissions.save(submission);
        if (saved.getStatus() == ExamSubmissionStatus.FAILED) {
            audit.failure(command.teacherId(), AuditAction.EXAM_PROCESSED, "ExamSubmission", saved.getId(),
                    saved.getStatusDetail());
        } else {
            audit.success(command.teacherId(), AuditAction.EXAM_PROCESSED, "ExamSubmission", saved.getId(),
                    "status " + saved.getStatus() + ", score " + saved.getScore() + ", finalGrade " + saved.getFinalGrade());
        }
        return assembler.assemble(saved);
    }

    private void applyReading(ExamSubmission submission, Exam exam, GradingScale scale, BigDecimal maximumScore,
                              List<DetectedAnswer> detected, LocalDateTime now) {
        if (detected.size() != exam.getNumberOfQuestions()) {
            throw new AnswerSheetProcessingException("SHEET_READ_ERROR", "The sheet reading does not match the exam");
        }
        List<ExamAnswer> answers = new ArrayList<>();
        for (DetectedAnswer d : detected) {
            ExamQuestion question = exam.questionByNumber(d.questionNumber());
            answers.add(ExamAnswer.builder().questionId(question.getId()).selectedOption(d.selectedOption())
                    .detectionStatus(d.status()).detectionConfidence(d.confidence()).build());
        }
        submission.setAnswers(answers);
        BigDecimal score = ExamScorer.score(exam.getQuestions(), answers);
        submission.setScore(score);
        submission.setFinalGrade(scale.convert(score, maximumScore));
        submission.setProcessedAt(now);
        long toReview = answers.stream().filter(ExamAnswer::needsReview).count();
        submission.setStatus(toReview > 0 ? ExamSubmissionStatus.REVIEW_REQUIRED : ExamSubmissionStatus.PROCESSED);
        submission.setStatusDetail(toReview > 0 ? toReview + " answer(s) require manual review" : null);
    }

    private Student identifyStudent(SubmissionCommands.Submit command, ExamContext ctx, String qrText) {
        Student student;
        if (qrText != null) {
            QrPayload payload = QrPayload.parse(qrText);
            if (payload.examId() != ctx.exam().getId()) {
                throw new BusinessRuleException("QR_EXAM_MISMATCH", "The QR code belongs to a different exam");
            }
            student = students.findByStudentCode(payload.studentCode()).orElseThrow(
                    () -> new BusinessRuleException("QR_STUDENT_NOT_FOUND", "The QR code refers to an unknown student"));
            if (command.studentId() != null && !command.studentId().equals(student.getId())) {
                throw new BusinessRuleException("STUDENT_MISMATCH", "The QR code belongs to a different student");
            }
        } else if (command.studentId() != null) {
            student = students.findById(command.studentId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Student", command.studentId()));
        } else {
            throw new BusinessRuleException("QR_NOT_DETECTED",
                    "No QR code was detected; retake the photo or provide studentId to assign the sheet manually");
        }
        if (!studentGroups.existsActive(student.getId(), ctx.period().groupId())) {
            throw new BusinessRuleException("STUDENT_NOT_IN_GROUP",
                    "The student does not belong to the group of this exam's teaching period");
        }
        return student;
    }

    private static void requireImage(byte[] image) {
        boolean png = image != null && image.length > 8 && (image[0] & 0xFF) == 0x89 && image[1] == 'P' && image[2] == 'N';
        boolean jpeg = image != null && image.length > 3 && (image[0] & 0xFF) == 0xFF && (image[1] & 0xFF) == 0xD8;
        if (!png && !jpeg) {
            throw new InvalidRequestException("INVALID_IMAGE", "Only non-empty JPEG or PNG images are supported");
        }
    }
}
