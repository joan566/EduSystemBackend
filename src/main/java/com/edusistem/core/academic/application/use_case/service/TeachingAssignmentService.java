package com.edusistem.core.academic.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.TeachingAssignment;
import com.edusistem.core.academic.domain.inputports.ManageTeachingAssignmentUseCase;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingAssignmentRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingAssignmentView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;

public class TeachingAssignmentService implements ManageTeachingAssignmentUseCase {

    private final TeachingAssignmentRepositoryPort assignments;
    private final GroupRepositoryPort groups;
    private final SubjectRepositoryPort subjects;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public TeachingAssignmentService(TeachingAssignmentRepositoryPort assignments, GroupRepositoryPort groups,
                                     SubjectRepositoryPort subjects, OwnershipGuard guard, RecordAuditUseCase audit) {
        this.assignments = assignments;
        this.groups = groups;
        this.subjects = subjects;
        this.guard = guard;
        this.audit = audit;
    }

    /** El profesor siempre es el usuario autenticado; solo puede asignarse a sí mismo. */
    @Override
    @UseCaseTransactional
    public TeachingAssignmentView create(AcademicCommands.CreateTeachingAssignment command) {
        groups.findById(command.groupId()).orElseThrow(() -> ResourceNotFoundException.of("Group", command.groupId()));
        subjects.findById(command.subjectId()).orElseThrow(() -> ResourceNotFoundException.of("Subject", command.subjectId()));
        TeachingAssignment assignment = assignments.findByTeacherIdAndGroupIdAndSubjectId(
                command.teacherId(), command.groupId(), command.subjectId()).orElse(null);
        if (assignment != null && assignment.isActive()) {
            throw new ConflictException("TEACHING_ASSIGNMENT_ALREADY_EXISTS",
                    "You already have an active assignment for this group and subject");
        }
        if (assignment == null) {
            assignment = TeachingAssignment.builder().teacherId(command.teacherId()).groupId(command.groupId())
                    .subjectId(command.subjectId()).build();
        }
        assignment.setActive(true); // reactiva una asignación inactiva (la unicidad de BD incluye inactivas)
        TeachingAssignment saved = assignments.save(assignment);
        audit.success(command.teacherId(), AuditAction.CREATE, "TeachingAssignment", saved.getId(), null);
        return get(command.teacherId(), saved.getId());
    }

    @Override
    @UseCaseTransactional
    public TeachingAssignmentView setActive(Long teacherId, Long assignmentId, boolean active) {
        guard.requireTeachingAssignment(teacherId, assignmentId);
        TeachingAssignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingAssignment", assignmentId));
        assignment.setActive(active);
        assignments.save(assignment);
        audit.success(teacherId, AuditAction.UPDATE, "TeachingAssignment", assignmentId, "active=" + active);
        return get(teacherId, assignmentId);
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long assignmentId) {
        guard.requireTeachingAssignment(teacherId, assignmentId);
        if (assignments.hasTeachingPeriods(assignmentId)) {
            throw new ConflictException("TEACHING_ASSIGNMENT_HAS_PERIODS",
                    "The assignment has academic periods configured; deactivate it instead of deleting it");
        }
        assignments.deleteById(assignmentId);
        audit.success(teacherId, AuditAction.DELETE, "TeachingAssignment", assignmentId, null);
    }

    @Override
    public TeachingAssignmentView get(Long teacherId, Long assignmentId) {
        guard.requireTeachingAssignment(teacherId, assignmentId);
        return assignments.findViewById(assignmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingAssignment", assignmentId));
    }

    @Override
    public PageResult<TeachingAssignmentView> list(Long teacherId, Long groupId, Long subjectId, Boolean active,
                                                   PageQuery page) {
        return assignments.findViewsByTeacherId(teacherId, groupId, subjectId, active, page);
    }
}
