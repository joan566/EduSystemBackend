package com.edusistem.core.activity.application.use_case.service;

import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.entity.Activity;
import com.edusistem.core.activity.domain.inputports.ManageActivityUseCase;
import com.edusistem.core.activity.domain.outputports.ActivityGradeRepositoryPort;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.application.use_case.dtos.EvaluationCommands;
import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.enums.EvaluationCategoryCode;
import com.edusistem.core.evaluation.domain.inputports.CreateEvaluationUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public class ActivityService implements ManageActivityUseCase {

    private final ActivityRepositoryPort activities;
    private final ActivityGradeRepositoryPort grades;
    private final EvaluationRepositoryPort evaluations;
    private final CreateEvaluationUseCase createEvaluation;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public ActivityService(ActivityRepositoryPort activities, ActivityGradeRepositoryPort grades,
                           EvaluationRepositoryPort evaluations, CreateEvaluationUseCase createEvaluation,
                           OwnershipGuard guard, RecordAuditUseCase audit) {
        this.activities = activities;
        this.grades = grades;
        this.evaluations = evaluations;
        this.createEvaluation = createEvaluation;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public ActivityView create(ActivityCommands.Create command) {
        Evaluation evaluation = createEvaluation.create(new EvaluationCommands.Create(command.teacherId(),
                command.teachingPeriodId(), EvaluationCategoryCode.ACTIVITIES, command.name(), command.description(),
                command.evaluationDate(), command.maximumScore()));
        Activity saved = activities.save(Activity.builder().evaluationId(evaluation.getId())
                .activityType(blankToNull(command.activityType())).build());
        audit.success(command.teacherId(), AuditAction.CREATE, "Activity", saved.getId(), evaluation.getName());
        return get(command.teacherId(), saved.getId());
    }

    @Override
    public ActivityView get(Long teacherId, Long activityId) {
        guard.requireActivity(teacherId, activityId);
        return activities.findViewById(activityId).orElseThrow(() -> ResourceNotFoundException.of("Activity", activityId));
    }

    @Override
    @UseCaseTransactional
    public ActivityView update(ActivityCommands.Update command) {
        guard.requireActivity(command.teacherId(), command.activityId());
        Activity activity = activities.findById(command.activityId())
                .orElseThrow(() -> ResourceNotFoundException.of("Activity", command.activityId()));
        Evaluation evaluation = evaluations.findById(activity.getEvaluationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation", activity.getEvaluationId()));
        if (command.maximumScore() != null && command.maximumScore().compareTo(evaluation.getMaximumScore()) != 0) {
            grades.findMaximumGrade(activity.getId()).ifPresent(max -> {
                if (max.compareTo(command.maximumScore()) > 0) {
                    throw new ConflictException("MAXIMUM_SCORE_BELOW_GRADES",
                            "Some registered grades exceed the new maximum score");
                }
            });
            evaluation.setMaximumScore(command.maximumScore());
        }
        evaluation.updateDetails(command.name(), command.description(), command.evaluationDate());
        evaluation.validate();
        evaluations.save(evaluation);
        activity.setActivityType(blankToNull(command.activityType()));
        activities.save(activity);
        audit.success(command.teacherId(), AuditAction.UPDATE, "Activity", activity.getId(), evaluation.getName());
        return get(command.teacherId(), activity.getId());
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long activityId) {
        guard.requireActivity(teacherId, activityId);
        Activity activity = activities.findById(activityId)
                .orElseThrow(() -> ResourceNotFoundException.of("Activity", activityId));
        if (grades.existsByActivityId(activityId)) {
            throw new ConflictException("ACTIVITY_HAS_GRADES", "The activity has grades and cannot be deleted");
        }
        activities.deleteById(activityId);
        evaluations.deleteById(activity.getEvaluationId());
        audit.success(teacherId, AuditAction.DELETE, "Activity", activityId, null);
    }

    @Override
    public PageResult<ActivityView> search(Long teacherId, Long teachingPeriodId, PageQuery page) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return activities.findViewsByTeachingPeriodId(teachingPeriodId, page);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.trim().length() > 50) {
            throw new InvalidRequestException("INVALID_ACTIVITY_TYPE", "activityType must have at most 50 characters");
        }
        return value.trim();
    }
}
