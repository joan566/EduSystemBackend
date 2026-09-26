package com.edusistem.core.activity.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.entity.Activity;
import com.edusistem.core.activity.domain.entity.ActivityGrade;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.outputports.ActivityGradeRepositoryPort;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.activity.domain.vo.StudentGradeView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ActivityGradeService implements GradeActivityUseCase {

    private final ActivityRepositoryPort activities;
    private final ActivityGradeRepositoryPort grades;
    private final EvaluationRepositoryPort evaluations;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final StudentRepositoryPort students;
    private final StudentGroupRepositoryPort studentGroups;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    public ActivityGradeService(ActivityRepositoryPort activities, ActivityGradeRepositoryPort grades,
                                EvaluationRepositoryPort evaluations, TeachingPeriodRepositoryPort teachingPeriods,
                                StudentRepositoryPort students, StudentGroupRepositoryPort studentGroups,
                                OwnershipGuard guard, RecordAuditUseCase audit, Clock clock) {
        this.activities = activities;
        this.grades = grades;
        this.evaluations = evaluations;
        this.teachingPeriods = teachingPeriods;
        this.students = students;
        this.studentGroups = studentGroups;
        this.guard = guard;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @UseCaseTransactional
    public List<StudentGradeView> recordGrades(ActivityCommands.RecordGrades command) {
        guard.requireActivity(command.teacherId(), command.activityId());
        if (command.grades() == null || command.grades().isEmpty()) {
            throw new InvalidRequestException("GRADES_REQUIRED", "At least one grade is required");
        }
        Activity activity = load(command.activityId());
        Evaluation evaluation = evaluationOf(activity);
        TeachingPeriodView period = periodOf(evaluation);
        Map<Long, ActivityGrade> existing = grades.findByActivityId(activity.getId()).stream()
                .collect(Collectors.toMap(ActivityGrade::getStudentId, Function.identity()));

        Set<Long> seen = new HashSet<>();
        LocalDateTime now = LocalDateTime.now(clock);
        List<ActivityGrade> toSave = new ArrayList<>();
        StringBuilder details = new StringBuilder();
        for (ActivityCommands.GradeInput input : command.grades()) {
            if (input.studentId() == null || !seen.add(input.studentId())) {
                throw new InvalidRequestException("INVALID_GRADES", "Each student must appear once with a valid studentId");
            }
            ActivityGrade.validateGrade(input.grade(), evaluation.getMaximumScore());
            if (!studentGroups.existsActive(input.studentId(), period.groupId())) {
                throw new InvalidRequestException("STUDENT_NOT_IN_GROUP",
                        "Student " + input.studentId() + " does not belong to the group of this activity");
            }
            ActivityGrade grade = existing.getOrDefault(input.studentId(),
                    ActivityGrade.builder().activityId(activity.getId()).studentId(input.studentId()).build());
            details.append(input.studentId()).append(": ").append(grade.getGrade()).append("->").append(input.grade()).append("; ");
            grade.setGrade(input.grade());
            grade.setComment(input.comment() == null || input.comment().isBlank() ? null : input.comment().trim());
            grade.setGradedAt(now);
            toSave.add(grade);
        }
        grades.saveAll(toSave);
        audit.success(command.teacherId(), AuditAction.GRADE_UPDATED, "Activity", activity.getId(), details.toString());
        return listGrades(command.teacherId(), activity.getId());
    }

    @Override
    public List<StudentGradeView> listGrades(Long teacherId, Long activityId) {
        guard.requireActivity(teacherId, activityId);
        Activity activity = load(activityId);
        TeachingPeriodView period = periodOf(evaluationOf(activity));
        Map<Long, ActivityGrade> byStudent = grades.findByActivityId(activityId).stream()
                .collect(Collectors.toMap(ActivityGrade::getStudentId, Function.identity()));
        List<StudentGradeView> views = new ArrayList<>();
        for (Student s : students.findActiveByGroupId(period.groupId())) {
            ActivityGrade g = byStudent.get(s.getId());
            views.add(new StudentGradeView(s.getId(), s.getStudentCode(), s.fullName(),
                    g == null ? null : g.getGrade(), g == null ? null : g.getComment(), g == null ? null : g.getGradedAt()));
        }
        return views;
    }

    private Activity load(Long activityId) {
        return activities.findById(activityId).orElseThrow(() -> ResourceNotFoundException.of("Activity", activityId));
    }

    private Evaluation evaluationOf(Activity activity) {
        return evaluations.findById(activity.getEvaluationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation", activity.getEvaluationId()));
    }

    private TeachingPeriodView periodOf(Evaluation evaluation) {
        return teachingPeriods.findViewById(evaluation.getTeachingPeriodId())
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", evaluation.getTeachingPeriodId()));
    }
}
