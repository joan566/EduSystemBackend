package com.edusistem.core.gradebook.application.use_case.service;

import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.audit.domain.vo.AuditTarget;
import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.entity.RubricCriterion;
import com.edusistem.core.gradebook.domain.entity.RubricScore;
import com.edusistem.core.gradebook.domain.inputports.ManageRubricUseCase;
import com.edusistem.core.gradebook.domain.inputports.QueryGradebookUseCase;
import com.edusistem.core.gradebook.domain.outputports.GradebookQueryPort;
import com.edusistem.core.gradebook.domain.outputports.GradebookQueryPort.EvaluationKind;
import com.edusistem.core.gradebook.domain.outputports.RubricRepositoryPort;
import com.edusistem.core.gradebook.domain.service.GradebookCalculator;
import com.edusistem.core.gradebook.domain.vo.EvaluationGradeDetail;
import com.edusistem.core.gradebook.domain.vo.EvaluationType;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rúbricas de las actividades. Los criterios suman 100 %; calificar con la rúbrica guarda el puntaje de cada criterio
 * (sobre el puntaje máximo de la actividad) y registra como nota de la actividad Σ puntaje × peso / 100, así que la
 * nota del periodo no cambia de regla.
 */
public class RubricService implements ManageRubricUseCase {

    static final int MAX_CRITERIA = 10;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final GradebookQueryPort query;
    private final RubricRepositoryPort rubrics;
    private final GradeActivityUseCase activityGrades;
    private final QueryGradebookUseCase gradebook;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public RubricService(GradebookQueryPort query, RubricRepositoryPort rubrics, GradeActivityUseCase activityGrades,
                         QueryGradebookUseCase gradebook, OwnershipGuard guard, RecordAuditUseCase audit) {
        this.query = query;
        this.rubrics = rubrics;
        this.activityGrades = activityGrades;
        this.gradebook = gradebook;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    public List<RubricCriterion> get(Long teacherId, Long evaluationId) {
        guard.requireEvaluation(teacherId, evaluationId);
        return rubrics.findCriteria(evaluationId);
    }

    @Override
    @UseCaseTransactional
    public List<RubricCriterion> save(GradebookCommands.SaveRubric command) {
        guard.requireEvaluation(command.teacherId(), command.evaluationId());
        EvaluationKind kind = activityKind(command.evaluationId());
        List<GradebookCommands.CriterionInput> inputs = command.criteria() == null ? List.of() : command.criteria();
        if (inputs.isEmpty() || inputs.size() > MAX_CRITERIA) {
            throw new InvalidRequestException("INVALID_RUBRIC", "A rubric needs between 1 and " + MAX_CRITERIA + " criteria");
        }
        Map<Long, RubricCriterion> existing = rubrics.findCriteria(command.evaluationId()).stream()
                .collect(Collectors.toMap(RubricCriterion::getId, Function.identity()));
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> kept = new HashSet<>();
        List<RubricCriterion> toSave = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            String name = input.name() == null ? "" : input.name().trim();
            if (name.isEmpty() || name.length() > 150) {
                throw new InvalidRequestException("INVALID_RUBRIC", "Each criterion needs a name of up to 150 characters");
            }
            if (input.weight() == null || input.weight().signum() <= 0 || input.weight().compareTo(HUNDRED) > 0) {
                throw new InvalidRequestException("INVALID_RUBRIC", "Each criterion weight must be greater than 0 and at most 100");
            }
            RubricCriterion criterion;
            if (input.id() == null) {
                criterion = RubricCriterion.builder().evaluationId(command.evaluationId()).build();
            } else {
                criterion = existing.get(input.id());
                if (criterion == null || !kept.add(input.id())) {
                    throw new InvalidRequestException("UNKNOWN_CRITERION",
                            "Criterion " + input.id() + " does not belong to this rubric");
                }
            }
            criterion.setPosition(i);
            criterion.setName(name);
            criterion.setWeight(input.weight());
            total = total.add(input.weight());
            toSave.add(criterion);
        }
        if (total.compareTo(HUNDRED) != 0) {
            throw new InvalidRequestException("RUBRIC_WEIGHTS_MUST_SUM_100",
                    "The criteria weights add up to " + total.stripTrailingZeros().toPlainString() + "%, not 100%");
        }
        rubrics.deleteCriteria(existing.keySet().stream().filter(id -> !kept.contains(id)).toList());
        List<RubricCriterion> saved = rubrics.saveCriteria(toSave);
        audit.success(command.teacherId(), existing.isEmpty() ? AuditAction.CREATE : AuditAction.UPDATE,
                AuditTarget.inTeachingPeriod("Rubric", command.evaluationId(), kind.teachingPeriodId(), "Rúbrica"),
                saved.size() + " criteria");
        return saved;
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long evaluationId) {
        guard.requireEvaluation(teacherId, evaluationId);
        EvaluationKind kind = activityKind(evaluationId);
        List<Long> ids = rubrics.findCriteria(evaluationId).stream().map(RubricCriterion::getId).toList();
        if (ids.isEmpty()) {
            return;
        }
        rubrics.deleteCriteria(ids);
        audit.success(teacherId, AuditAction.DELETE,
                AuditTarget.inTeachingPeriod("Rubric", evaluationId, kind.teachingPeriodId(), "Rúbrica"), "rubric removed");
    }

    @Override
    @UseCaseTransactional
    public EvaluationGradeDetail score(GradebookCommands.ScoreWithRubric command) {
        guard.requireEvaluation(command.teacherId(), command.evaluationId());
        guard.requireStudent(command.teacherId(), command.studentId());
        EvaluationKind kind = activityKind(command.evaluationId());
        List<RubricCriterion> criteria = rubrics.findCriteria(command.evaluationId());
        if (criteria.isEmpty()) {
            throw new ConflictException("RUBRIC_REQUIRED", "This activity has no rubric");
        }
        Map<Long, BigDecimal> byCriterion = new java.util.HashMap<>();
        for (var input : command.scores() == null ? List.<GradebookCommands.ScoreInput>of() : command.scores()) {
            if (input.criterionId() == null || byCriterion.put(input.criterionId(), input.score()) != null) {
                throw new InvalidRequestException("INVALID_RUBRIC_SCORES", "Each criterion must be scored once");
            }
            if (input.score() == null || input.score().signum() < 0 || input.score().compareTo(kind.maximumScore()) > 0) {
                throw new InvalidRequestException("INVALID_RUBRIC_SCORES",
                        "Each score must be between 0 and " + kind.maximumScore().stripTrailingZeros().toPlainString());
            }
        }
        if (!byCriterion.keySet().equals(criteria.stream().map(RubricCriterion::getId).collect(Collectors.toSet()))) {
            throw new InvalidRequestException("INVALID_RUBRIC_SCORES", "Every criterion of the rubric must be scored");
        }

        Map<Long, RubricScore> existing = rubrics.findScores(command.evaluationId(), command.studentId()).stream()
                .collect(Collectors.toMap(RubricScore::getCriterionId, Function.identity()));
        List<RubricScore> toSave = new ArrayList<>();
        for (RubricCriterion criterion : criteria) {
            RubricScore score = existing.getOrDefault(criterion.getId(), RubricScore.builder()
                    .criterionId(criterion.getId()).studentId(command.studentId()).build());
            score.setScore(byCriterion.get(criterion.getId()));
            toSave.add(score);
        }
        rubrics.saveScores(toSave);

        BigDecimal grade = GradebookCalculator.rubricGrade(
                criteria.stream().map(c -> byCriterion.get(c.getId())).toList(),
                criteria.stream().map(RubricCriterion::getWeight).toList());
        String comment = command.comment();
        if (comment == null) {
            // Sin comentario en la petición, se conserva el que ya tenía la nota.
            comment = gradebook.gradeDetail(command.teacherId(), command.evaluationId(), command.studentId())
                    .entry().row().comment();
        }
        activityGrades.recordGrades(new ActivityCommands.RecordGrades(command.teacherId(), kind.activityId(),
                List.of(new ActivityCommands.GradeInput(command.studentId(), grade, comment))));
        return gradebook.gradeDetail(command.teacherId(), command.evaluationId(), command.studentId());
    }

    private EvaluationKind activityKind(Long evaluationId) {
        EvaluationKind kind = query.findKind(evaluationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation", evaluationId));
        if (kind.type() != EvaluationType.ACTIVITY) {
            throw new ConflictException("RUBRIC_NOT_SUPPORTED", "Only activities can be graded with a rubric");
        }
        return kind;
    }
}
