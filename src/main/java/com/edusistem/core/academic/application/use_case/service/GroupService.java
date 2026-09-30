package com.edusistem.core.academic.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.inputports.ManageGroupUseCase;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

/** Grupos propios del profesor, siempre dentro de un grado suyo; los de otros profesores responden 404. */
public class GroupService implements ManageGroupUseCase {

    private final GroupRepositoryPort groups;
    private final GradeRepositoryPort grades;
    private final RecordAuditUseCase audit;

    public GroupService(GroupRepositoryPort groups, GradeRepositoryPort grades, RecordAuditUseCase audit) {
        this.groups = groups;
        this.grades = grades;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public GroupView create(AcademicCommands.CreateGroup command) {
        grades.findById(command.gradeId()).filter(g -> g.getTeacherId().equals(command.teacherId()))
                .orElseThrow(() -> ResourceNotFoundException.of("Grade", command.gradeId()));
        String name = command.name().trim();
        requireUnique(command.gradeId(), name, command.academicYear(), null);
        Group saved = groups.save(Group.builder().teacherId(command.teacherId()).gradeId(command.gradeId()).name(name)
                .academicYear(command.academicYear()).build());
        audit.success(command.teacherId(), AuditAction.CREATE, "Group", saved.getId(), name + " " + saved.getAcademicYear());
        return get(command.teacherId(), saved.getId());
    }

    @Override
    @UseCaseTransactional
    public GroupView update(AcademicCommands.UpdateGroup command) {
        Group group = owned(command.teacherId(), command.groupId());
        String name = command.name().trim();
        requireUnique(group.getGradeId(), name, command.academicYear(), group.getId());
        group.setName(name);
        group.setAcademicYear(command.academicYear());
        groups.save(group);
        audit.success(command.teacherId(), AuditAction.UPDATE, "Group", group.getId(), name + " " + command.academicYear());
        return get(command.teacherId(), group.getId());
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long groupId) {
        Group group = owned(teacherId, groupId);
        if (groups.hasDependents(groupId)) {
            throw new ConflictException("GROUP_HAS_DEPENDENTS",
                    "The group has enrolled students or teaching assignments and cannot be deleted");
        }
        groups.deleteById(groupId);
        audit.success(teacherId, AuditAction.DELETE, "Group", groupId, group.getName());
    }

    @Override
    public GroupView get(Long teacherId, Long groupId) {
        owned(teacherId, groupId);
        return groups.findViewById(groupId).orElseThrow(() -> ResourceNotFoundException.of("Group", groupId));
    }

    @Override
    public PageResult<GroupView> search(Long teacherId, Long gradeId, Integer academicYear, PageQuery page) {
        return groups.search(teacherId, gradeId, academicYear, page);
    }

    private Group owned(Long teacherId, Long groupId) {
        return groups.findById(groupId).filter(g -> g.getTeacherId().equals(teacherId))
                .orElseThrow(() -> ResourceNotFoundException.of("Group", groupId));
    }

    private void requireUnique(Long gradeId, String name, int year, Long currentId) {
        groups.findByGradeIdAndNameAndAcademicYear(gradeId, name, year).filter(g -> !g.getId().equals(currentId))
                .ifPresent(g -> {
                    throw new ConflictException("GROUP_ALREADY_EXISTS",
                            "The group '" + name + "' already exists for that grade and academic year");
                });
    }
}
