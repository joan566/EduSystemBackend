package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.application.use_case.dtos.EvaluationCommands;
import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.enums.EvaluationCategoryCode;
import com.edusistem.core.evaluation.domain.inputports.CreateEvaluationUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.exam.application.use_case.dtos.ExamCommands;
import com.edusistem.core.exam.application.use_case.service.ExamContextLoader.ExamContext;
import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamQuestionOption;
import com.edusistem.core.exam.domain.inputports.ManageExamUseCase;
import com.edusistem.core.exam.domain.outputports.ExamRepositoryPort;
import com.edusistem.core.exam.domain.vo.ExamDetails;
import com.edusistem.core.exam.domain.vo.ExamView;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ExamService implements ManageExamUseCase {

    private final ExamRepositoryPort exams;
    private final EvaluationRepositoryPort evaluations;
    private final CreateEvaluationUseCase createEvaluation;
    private final ExamContextLoader loader;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public ExamService(ExamRepositoryPort exams, EvaluationRepositoryPort evaluations,
                       CreateEvaluationUseCase createEvaluation, ExamContextLoader loader, OwnershipGuard guard,
                       RecordAuditUseCase audit) {
        this.exams = exams;
        this.evaluations = evaluations;
        this.createEvaluation = createEvaluation;
        this.loader = loader;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public ExamDetails create(ExamCommands.Create command) {
        guard.requireTeachingPeriod(command.teacherId(), command.teachingPeriodId());
        Exam.validateNumberOfQuestions(command.numberOfQuestions());
        boolean withQuestions = command.questions() != null && !command.questions().isEmpty();
        if (withQuestions && command.questions().size() != command.numberOfQuestions()) {
            throw new InvalidRequestException("QUESTION_COUNT_MISMATCH",
                    "numberOfQuestions does not match the number of questions provided");
        }
        BigDecimal maximumScore = command.maximumScore() != null ? command.maximumScore()
                : BigDecimal.valueOf(command.numberOfQuestions());
        Evaluation evaluation = createEvaluation.create(new EvaluationCommands.Create(command.teacherId(),
                command.teachingPeriodId(), EvaluationCategoryCode.EXAMS, command.name(), command.description(),
                command.evaluationDate(), maximumScore));
        Exam exam = Exam.builder().evaluationId(evaluation.getId()).numberOfQuestions(command.numberOfQuestions()).build();
        if (withQuestions) {
            applyQuestions(exam, evaluation, command.questions());
        }
        Exam saved = exams.save(exam);
        audit.success(command.teacherId(), AuditAction.CREATE, "Exam", saved.getId(), evaluation.getName());
        return get(command.teacherId(), saved.getId());
    }

    @Override
    public ExamDetails get(Long teacherId, Long examId) {
        ExamContext ctx = loader.load(teacherId, examId);
        ExamView view = exams.findViewById(examId).orElseThrow(() -> ResourceNotFoundException.of("Exam", examId));
        return new ExamDetails(view, ctx.exam());
    }

    @Override
    @UseCaseTransactional
    public ExamDetails update(ExamCommands.Update command) {
        ExamContext ctx = loader.load(command.teacherId(), command.examId());
        Evaluation evaluation = ctx.evaluation();
        evaluation.updateDetails(command.name(), command.description(), command.evaluationDate());
        evaluation.validate();
        evaluations.save(evaluation);
        audit.success(command.teacherId(), AuditAction.UPDATE, "Exam", command.examId(), evaluation.getName());
        return get(command.teacherId(), command.examId());
    }

    @Override
    @UseCaseTransactional
    public ExamDetails replaceQuestions(ExamCommands.ReplaceQuestions command) {
        ExamContext ctx = loader.load(command.teacherId(), command.examId());
        if (exams.hasSubmissions(command.examId())) {
            throw new ConflictException("EXAM_HAS_SUBMISSIONS",
                    "The questions cannot change because answer sheets were already processed");
        }
        applyQuestions(ctx.exam(), ctx.evaluation(), command.questions());
        exams.save(ctx.exam());
        audit.success(command.teacherId(), AuditAction.UPDATE, "Exam", command.examId(),
                "questions replaced (" + ctx.exam().getNumberOfQuestions() + ")");
        return get(command.teacherId(), command.examId());
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long examId) {
        ExamContext ctx = loader.load(teacherId, examId);
        if (exams.hasSubmissions(examId)) {
            throw new ConflictException("EXAM_HAS_SUBMISSIONS", "The exam has submissions and cannot be deleted");
        }
        exams.deleteById(examId);
        evaluations.deleteById(ctx.evaluation().getId());
        audit.success(teacherId, AuditAction.DELETE, "Exam", examId, ctx.evaluation().getName());
    }

    @Override
    public PageResult<ExamView> search(Long teacherId, Long teachingPeriodId, PageQuery page) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return exams.findViewsByTeachingPeriodId(teachingPeriodId, page);
    }

    /** Construye las preguntas, aplica puntos por defecto y ajusta el puntaje máximo de la evaluación a su suma. */
    private void applyQuestions(Exam exam, Evaluation evaluation, List<ExamCommands.QuestionInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new InvalidRequestException("QUESTIONS_REQUIRED", "At least one question is required");
        }
        BigDecimal defaultPoints = evaluation.getMaximumScore()
                .divide(BigDecimal.valueOf(inputs.size()), 2, RoundingMode.HALF_UP);
        List<ExamQuestion> questions = new ArrayList<>();
        for (ExamCommands.QuestionInput in : inputs) {
            List<ExamQuestionOption> options = new ArrayList<>();
            if (in.options() != null) {
                in.options().forEach(o -> options.add(ExamQuestionOption.builder()
                        .optionLetter(o.letter() == null ? null : o.letter().trim().toUpperCase())
                        .optionText(o.text()).build()));
            }
            questions.add(ExamQuestion.builder().questionNumber(in.questionNumber()).statement(in.statement())
                    .correctOption(in.correctOption() == null ? null : in.correctOption().trim().toUpperCase())
                    .points(in.points() != null ? in.points() : defaultPoints).options(options).build());
        }
        exam.replaceQuestions(questions);
        evaluation.setMaximumScore(exam.totalPoints());
        if (evaluation.getId() != null) {
            evaluations.save(evaluation);
        }
    }
}
