package com.edusistem.core.gradebook.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.audit.domain.vo.AuditTarget;
import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.entity.RubricScore;
import com.edusistem.core.gradebook.domain.entity.StudentObservation;
import com.edusistem.core.gradebook.domain.inputports.ManageStudentObservationUseCase;
import com.edusistem.core.gradebook.domain.inputports.QueryGradebookUseCase;
import com.edusistem.core.gradebook.domain.outputports.GradeAttachmentRepositoryPort;
import com.edusistem.core.gradebook.domain.outputports.GradebookQueryPort;
import com.edusistem.core.gradebook.domain.outputports.RubricRepositoryPort;
import com.edusistem.core.gradebook.domain.outputports.StudentObservationRepositoryPort;
import com.edusistem.core.gradebook.domain.service.GradebookCalculator;
import com.edusistem.core.gradebook.domain.vo.EvaluationGradeDetail;
import com.edusistem.core.gradebook.domain.vo.GradebookEntry;
import com.edusistem.core.gradebook.domain.vo.RubricCriterionScore;
import com.edusistem.core.gradebook.domain.vo.StudentGradeReport;
import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/** Notas de un estudiante evaluación por evaluación, el detalle de cada nota y las observaciones del docente. */
public class GradebookService implements QueryGradebookUseCase, ManageStudentObservationUseCase {

    static final int MAX_OBSERVATION_LENGTH = 1000;

    private final GradebookQueryPort query;
    private final GradingConfigurationRepositoryPort configurations;
    private final GradingScaleRepositoryPort scales;
    private final EvaluationCategoryRepositoryPort categories;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final StudentRepositoryPort students;
    private final StudentGroupRepositoryPort studentGroups;
    private final RubricRepositoryPort rubrics;
    private final GradeAttachmentRepositoryPort attachments;
    private final StudentObservationRepositoryPort observations;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public GradebookService(GradebookQueryPort query, GradingConfigurationRepositoryPort configurations,
                            GradingScaleRepositoryPort scales, EvaluationCategoryRepositoryPort categories,
                            TeachingPeriodRepositoryPort teachingPeriods, StudentRepositoryPort students,
                            StudentGroupRepositoryPort studentGroups, RubricRepositoryPort rubrics,
                            GradeAttachmentRepositoryPort attachments, StudentObservationRepositoryPort observations,
                            OwnershipGuard guard, RecordAuditUseCase audit) {
        this.query = query;
        this.configurations = configurations;
        this.scales = scales;
        this.categories = categories;
        this.teachingPeriods = teachingPeriods;
        this.students = students;
        this.studentGroups = studentGroups;
        this.rubrics = rubrics;
        this.attachments = attachments;
        this.observations = observations;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    public StudentGradeReport studentReport(Long teacherId, Long teachingPeriodId, Long studentId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        guard.requireStudent(teacherId, studentId);
        Student student = enrolledStudent(teachingPeriodId, studentId);
        GradingConfiguration configuration = configurations.findByTeachingPeriodId(teachingPeriodId).orElse(null);
        GradingScale scale = configuration == null ? null : scales.findById(configuration.getGradingScaleId())
                .orElseThrow(() -> ResourceNotFoundException.of("GradingScale", configuration.getGradingScaleId()));
        Map<Long, String> categoryNames = categories.findAll().stream()
                .collect(Collectors.toMap(EvaluationCategory::getId, EvaluationCategory::getName));

        var result = GradebookCalculator.calculate(studentId, query.findRows(teachingPeriodId, studentId),
                configuration, scale, categoryNames, query.evaluationIdsWithRubric(teachingPeriodId),
                attachments.evaluationIdsWithAttachment(teachingPeriodId, studentId));
        boolean complete = configuration != null && configuration.isComplete();
        return new StudentGradeReport(teachingPeriodId, student, scale,
                configuration == null ? null : configuration.getPassingGrade(),
                configuration == null ? BigDecimal.ZERO : configuration.totalWeight(), complete,
                result.periodGrade(), result.score(),
                complete ? configuration.isPassing(result.periodGrade()) : null, result.categories(),
                result.entries(), observations.find(teachingPeriodId, studentId).orElse(null));
    }

    @Override
    public EvaluationGradeDetail gradeDetail(Long teacherId, Long evaluationId, Long studentId) {
        guard.requireEvaluation(teacherId, evaluationId);
        var kind = query.findKind(evaluationId).orElseThrow(() -> ResourceNotFoundException.of("Evaluation", evaluationId));
        StudentGradeReport report = studentReport(teacherId, kind.teachingPeriodId(), studentId);
        GradebookEntry entry = report.evaluations().stream()
                .filter(e -> e.row().evaluationId().equals(evaluationId)).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation", evaluationId));
        Map<Long, BigDecimal> scores = rubrics.findScores(evaluationId, studentId).stream()
                .collect(Collectors.toMap(RubricScore::getCriterionId, RubricScore::getScore));
        var rubric = rubrics.findCriteria(evaluationId).stream()
                .map(c -> new RubricCriterionScore(c, scores.get(c.getId()))).toList();
        return new EvaluationGradeDetail(kind.teachingPeriodId(), report.student(), report.scale(), entry, rubric,
                attachments.find(evaluationId, studentId).orElse(null));
    }

    @Override
    @UseCaseTransactional
    public StudentObservation save(GradebookCommands.SaveObservation command) {
        guard.requireTeachingPeriod(command.teacherId(), command.teachingPeriodId());
        guard.requireStudent(command.teacherId(), command.studentId());
        Student student = enrolledStudent(command.teachingPeriodId(), command.studentId());
        String text = command.text() == null ? "" : command.text().trim();
        if (text.length() > MAX_OBSERVATION_LENGTH) {
            throw new InvalidRequestException("OBSERVATION_TOO_LONG",
                    "The observation cannot exceed " + MAX_OBSERVATION_LENGTH + " characters");
        }
        StudentObservation existing = observations.find(command.teachingPeriodId(), command.studentId()).orElse(null);
        AuditTarget target = AuditTarget.inTeachingPeriod("StudentObservation", command.studentId(),
                command.teachingPeriodId(), AuditTarget.label("Observación", student.fullName()));
        if (text.isEmpty()) {
            if (existing != null) {
                observations.delete(existing.getId());
                audit.success(command.teacherId(), AuditAction.DELETE, target, "observation removed");
            }
            return null;
        }
        StudentObservation observation = existing != null ? existing : StudentObservation.builder()
                .teachingPeriodId(command.teachingPeriodId()).studentId(command.studentId()).build();
        observation.setText(text);
        StudentObservation saved = observations.save(observation);
        audit.success(command.teacherId(), existing == null ? AuditAction.CREATE : AuditAction.UPDATE, target,
                "observation saved");
        return saved;
    }

    /** El estudiante debe estar (o haber estado) matriculado en el grupo de la clase. */
    Student enrolledStudent(Long teachingPeriodId, Long studentId) {
        TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
        if (studentGroups.find(studentId, period.groupId()).isEmpty()) {
            throw new ResourceNotFoundException("STUDENT_NOT_IN_CLASS",
                    "Student " + studentId + " is not enrolled in the group of teaching period " + teachingPeriodId);
        }
        return students.findById(studentId).orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));
    }
}
