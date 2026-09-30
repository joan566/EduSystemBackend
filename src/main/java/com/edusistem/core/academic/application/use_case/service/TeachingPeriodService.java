package com.edusistem.core.academic.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.TeachingAssignment;
import com.edusistem.core.academic.domain.entity.TeachingPeriod;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodUseCase;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingAssignmentRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodSummary;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.audit.domain.vo.AuditTarget;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public class TeachingPeriodService implements ManageTeachingPeriodUseCase {

    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final TeachingAssignmentRepositoryPort assignments;
    private final AcademicPeriodRepositoryPort academicPeriods;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public TeachingPeriodService(TeachingPeriodRepositoryPort teachingPeriods, TeachingAssignmentRepositoryPort assignments,
                                 AcademicPeriodRepositoryPort academicPeriods, OwnershipGuard guard,
                                 RecordAuditUseCase audit) {
        this.teachingPeriods = teachingPeriods;
        this.assignments = assignments;
        this.academicPeriods = academicPeriods;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public TeachingPeriodView create(AcademicCommands.CreateTeachingPeriod command) {
        guard.requireTeachingAssignment(command.teacherId(), command.teachingAssignmentId());
        TeachingAssignment assignment = assignments.findById(command.teachingAssignmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingAssignment", command.teachingAssignmentId()));
        if (!assignment.isActive()) {
            throw new BusinessRuleException("TEACHING_ASSIGNMENT_INACTIVE", "The teaching assignment is not active");
        }
        academicPeriods.findById(command.academicPeriodId()).filter(p -> p.getTeacherId().equals(command.teacherId()))
                .orElseThrow(() -> ResourceNotFoundException.of("AcademicPeriod", command.academicPeriodId()));
        teachingPeriods.findByTeachingAssignmentIdAndAcademicPeriodId(command.teachingAssignmentId(),
                command.academicPeriodId()).ifPresent(tp -> {
            throw new ConflictException("TEACHING_PERIOD_ALREADY_EXISTS",
                    "The teaching assignment is already linked to that academic period");
        });
        TeachingPeriod saved = teachingPeriods.save(TeachingPeriod.builder()
                .teachingAssignmentId(command.teachingAssignmentId()).academicPeriodId(command.academicPeriodId()).build());
        TeachingPeriodView view = get(command.teacherId(), saved.getId());
        audit.success(command.teacherId(), AuditAction.CREATE, target(view), null);
        return view;
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long teachingPeriodId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        if (teachingPeriods.hasDependents(teachingPeriodId)) {
            throw new ConflictException("TEACHING_PERIOD_HAS_DATA",
                    "The teaching period has evaluations or a grading configuration and cannot be deleted");
        }
        TeachingPeriodView view = get(teacherId, teachingPeriodId);
        teachingPeriods.deleteById(teachingPeriodId);
        audit.success(teacherId, AuditAction.DELETE, target(view), null);
    }

    private static AuditTarget target(TeachingPeriodView view) {
        return AuditTarget.inTeachingPeriod("TeachingPeriod", view.id(), view.id(), view.label());
    }

    @Override
    public TeachingPeriodView get(Long teacherId, Long teachingPeriodId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
    }

    @Override
    public TeachingPeriodSummary summary(Long teacherId, Long teachingPeriodId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        return teachingPeriods.summarize(teachingPeriodId);
    }

    @Override
    public PageResult<TeachingPeriodView> list(Long teacherId, Long teachingAssignmentId, Long academicPeriodId,
                                               PageQuery page) {
        return teachingPeriods.findViewsByTeacherId(teacherId, teachingAssignmentId, academicPeriodId, page);
    }
}
