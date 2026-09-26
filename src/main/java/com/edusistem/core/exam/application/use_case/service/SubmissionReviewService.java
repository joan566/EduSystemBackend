package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.application.use_case.service.ExamContextLoader.ExamContext;
import com.edusistem.core.exam.domain.entity.ExamAnswer;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import com.edusistem.core.exam.domain.inputports.QuerySubmissionUseCase;
import com.edusistem.core.exam.domain.inputports.ReviewSubmissionUseCase;
import com.edusistem.core.exam.domain.outputports.ExamSubmissionRepositoryPort;
import com.edusistem.core.exam.domain.service.ExamScorer;
import com.edusistem.core.exam.domain.vo.ExamSubmissionSummary;
import com.edusistem.core.exam.domain.vo.ImageFile;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

/** Consulta y revisión manual. Toda edición se audita con el valor anterior y el nuevo (trazabilidad). */
public class SubmissionReviewService implements ReviewSubmissionUseCase, QuerySubmissionUseCase {

    private final ExamContextLoader loader;
    private final ExamSubmissionRepositoryPort submissions;
    private final SubmissionDetailsAssembler assembler;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;
    private final FileStoragePort storage;
    private final Clock clock;

    public SubmissionReviewService(ExamContextLoader loader, ExamSubmissionRepositoryPort submissions,
                                   SubmissionDetailsAssembler assembler, OwnershipGuard guard,
                                   RecordAuditUseCase audit, FileStoragePort storage, Clock clock) {
        this.loader = loader;
        this.submissions = submissions;
        this.assembler = assembler;
        this.guard = guard;
        this.audit = audit;
        this.storage = storage;
        this.clock = clock;
    }

    @Override
    public SubmissionDetails get(Long teacherId, Long examId, Long submissionId) {
        guard.requireExam(teacherId, examId);
        return assembler.assemble(findSubmission(examId, submissionId));
    }

    @Override
    public ImageFile image(Long teacherId, Long examId, Long submissionId) {
        guard.requireExam(teacherId, examId);
        ExamSubmission submission = findSubmission(examId, submissionId);
        String path = submission.getImagePath();
        if (path == null || !storage.exists(path)) {
            throw new ResourceNotFoundException("IMAGE_NOT_FOUND", "The original image is not available");
        }
        try {
            byte[] content = storage.read(path);
            boolean png = content.length > 3 && (content[0] & 0xFF) == 0x89 && content[1] == 'P';
            return new ImageFile(png ? "image/png" : "image/jpeg", content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public PageResult<ExamSubmissionSummary> search(Long teacherId, Long examId, ExamSubmissionStatus status, PageQuery page) {
        guard.requireExam(teacherId, examId);
        return submissions.findSummariesByExamId(examId, status, page);
    }

    @Override
    @UseCaseTransactional
    public SubmissionDetails updateAnswer(SubmissionCommands.UpdateAnswer command) {
        ExamContext ctx = loader.load(command.teacherId(), command.examId());
        ExamSubmission submission = findSubmission(command.examId(), command.submissionId());
        requireReviewable(submission);
        ExamQuestion question = ctx.exam().questionByNumber(command.questionNumber());
        if (question == null) {
            throw ResourceNotFoundException.of("Question", command.questionNumber());
        }
        String option = command.selectedOption() == null || command.selectedOption().isBlank() ? null
                : command.selectedOption().trim().toUpperCase(Locale.ROOT);
        if (option != null && !question.hasOption(option)) {
            throw new InvalidRequestException("INVALID_OPTION", "Question " + question.getQuestionNumber()
                    + " has no option " + option);
        }
        ExamAnswer answer = submission.getAnswers().stream().filter(a -> a.getQuestionId().equals(question.getId()))
                .findFirst().orElseThrow(() -> ResourceNotFoundException.of("Answer", command.questionNumber()));

        String before = describe(answer);
        BigDecimal scoreBefore = submission.getScore();
        BigDecimal gradeBefore = submission.getFinalGrade();
        answer.setSelectedOption(option);
        answer.setDetectionStatus(AnswerDetectionStatus.MANUAL);

        GradingScale scale = loader.requireScale(ctx.evaluation().getTeachingPeriodId());
        BigDecimal score = ExamScorer.score(ctx.exam().getQuestions(), submission.getAnswers());
        submission.setScore(score);
        submission.setFinalGrade(scale.convert(score, ctx.evaluation().getMaximumScore()));
        refreshStatus(submission);
        ExamSubmission saved = submissions.save(submission);

        audit.success(command.teacherId(), AuditAction.ANSWER_UPDATED, "ExamSubmission", saved.getId(),
                "Q" + question.getQuestionNumber() + ": " + before + " -> " + describe(answer)
                        + "; score " + scoreBefore + " -> " + saved.getScore()
                        + "; finalGrade " + gradeBefore + " -> " + saved.getFinalGrade() + reason(command.reason()));
        return assembler.assemble(saved);
    }

    @Override
    @UseCaseTransactional
    public SubmissionDetails updateFinalGrade(SubmissionCommands.UpdateFinalGrade command) {
        ExamContext ctx = loader.load(command.teacherId(), command.examId());
        ExamSubmission submission = findSubmission(command.examId(), command.submissionId());
        requireReviewable(submission);
        GradingScale scale = loader.requireScale(ctx.evaluation().getTeachingPeriodId());
        if (command.finalGrade() == null || !scale.contains(command.finalGrade())) {
            throw new InvalidRequestException("GRADE_OUT_OF_RANGE", "finalGrade must be between "
                    + scale.getMinimumValue().stripTrailingZeros().toPlainString() + " and "
                    + scale.getMaximumValue().stripTrailingZeros().toPlainString());
        }
        BigDecimal before = submission.getFinalGrade();
        submission.setFinalGrade(command.finalGrade().setScale(2, RoundingMode.HALF_UP));
        submission.setProcessedAt(LocalDateTime.now(clock));
        ExamSubmission saved = submissions.save(submission);
        audit.success(command.teacherId(), AuditAction.GRADE_UPDATED, "ExamSubmission", saved.getId(),
                "finalGrade " + before + " -> " + saved.getFinalGrade() + " (score " + saved.getScore() + ")"
                        + reason(command.reason()));
        return assembler.assemble(saved);
    }

    private ExamSubmission findSubmission(Long examId, Long submissionId) {
        return submissions.findById(submissionId).filter(s -> s.getExamId().equals(examId))
                .orElseThrow(() -> ResourceNotFoundException.of("ExamSubmission", submissionId));
    }

    private static void requireReviewable(ExamSubmission submission) {
        if (submission.getStatus() == ExamSubmissionStatus.FAILED || submission.getAnswers().isEmpty()) {
            throw new ConflictException("SUBMISSION_NOT_REVIEWABLE",
                    "The submission has no detected answers; upload the sheet again");
        }
    }

    private static void refreshStatus(ExamSubmission submission) {
        long pending = submission.getAnswers().stream().filter(ExamAnswer::needsReview).count();
        submission.setStatus(pending > 0 ? ExamSubmissionStatus.REVIEW_REQUIRED : ExamSubmissionStatus.PROCESSED);
        submission.setStatusDetail(pending > 0 ? pending + " answer(s) require manual review" : null);
    }

    private static String describe(ExamAnswer a) {
        return (a.getSelectedOption() == null ? "none" : a.getSelectedOption()) + "(" + a.getDetectionStatus()
                + (a.getDetectionConfidence() == null ? "" : ", conf " + a.getDetectionConfidence()) + ")";
    }

    private static String reason(String reason) {
        return reason == null || reason.isBlank() ? "" : "; reason: " + reason.trim();
    }
}
