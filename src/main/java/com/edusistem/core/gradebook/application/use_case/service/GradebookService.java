package com.edusistem.core.gradebook.application.use_case.service;

import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.gradebook.application.contracts.EnrolledStudentLookup;
import com.edusistem.core.gradebook.domain.entity.RubricScore;
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
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.student.domain.entity.Student;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/** Notas de un estudiante evaluación por evaluación y el detalle de cada nota. */
public class GradebookService implements QueryGradebookUseCase {

    private final GradebookQueryPort query;
    private final GradingConfigurationRepositoryPort configurations;
    private final GradingScaleRepositoryPort scales;
    private final EvaluationCategoryRepositoryPort categories;
    private final RubricRepositoryPort rubrics;
    private final GradeAttachmentRepositoryPort attachments;
    private final StudentObservationRepositoryPort observations;
    private final EnrolledStudentLookup enrolledStudents;
    private final OwnershipGuard guard;

    public GradebookService(GradebookQueryPort query, GradingConfigurationRepositoryPort configurations,
                            GradingScaleRepositoryPort scales, EvaluationCategoryRepositoryPort categories,
                            RubricRepositoryPort rubrics, GradeAttachmentRepositoryPort attachments,
                            StudentObservationRepositoryPort observations, EnrolledStudentLookup enrolledStudents,
                            OwnershipGuard guard) {
        this.query = query;
        this.configurations = configurations;
        this.scales = scales;
        this.categories = categories;
        this.rubrics = rubrics;
        this.attachments = attachments;
        this.observations = observations;
        this.enrolledStudents = enrolledStudents;
        this.guard = guard;
    }

    @Override
    public StudentGradeReport studentReport(Long teacherId, Long teachingPeriodId, Long studentId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        guard.requireStudent(teacherId, studentId);
        Student student = enrolledStudents.require(teachingPeriodId, studentId);
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
}
