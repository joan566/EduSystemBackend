package com.edusistem.core.academic.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.inputports.ManageGroupUseCase;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.CatalogUsagePort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService implements ManageGroupUseCase {

    private final GroupRepositoryPort groups;
    private final GradeRepositoryPort grades;
    private final CatalogUsagePort usage;
    private final RecordAuditUseCase audit;

    public GroupService(GroupRepositoryPort groups, GradeRepositoryPort grades, CatalogUsagePort usage,
                        RecordAuditUseCase audit) {
        this.groups = groups;
        this.grades = grades;
        this.usage = usage;
        this.audit = audit;
    }

    @Override
    @Transactional
    public GroupView create(AcademicCommands.CreateGroup command) {
        grades.findById(command.gradeId()).orElseThrow(() -> ResourceNotFoundException.of("Grade", command.gradeId()));
        String name = command.name().trim();
        requireUnique(command.gradeId(), name, command.academicYear(), null);
        Group saved = groups.save(Group.builder().gradeId(command.gradeId()).name(name)
                .academicYear(command.academicYear()).build());
        audit.success(command.actorId(), AuditAction.CREATE, "Group", saved.getId(), name + " " + saved.getAcademicYear());
        return get(saved.getId());
    }

    @Override
    @Transactional
    public GroupView update(AcademicCommands.UpdateGroup command) {
        Group group = groups.findById(command.groupId())
                .orElseThrow(() -> ResourceNotFoundException.of("Group", command.groupId()));
        if (usage.groupUsedByOtherTeachers(group.getId(), command.actorId())) {
            throw new ConflictException("CATALOG_ITEM_IN_USE", "The group is used by other teachers and cannot be modified");
        }
        String name = command.name().trim();
        requireUnique(group.getGradeId(), name, command.academicYear(), group.getId());
        group.setName(name);
        group.setAcademicYear(command.academicYear());
        groups.save(group);
        audit.success(command.actorId(), AuditAction.UPDATE, "Group", group.getId(), name + " " + command.academicYear());
        return get(group.getId());
    }

    @Override
    @Transactional
    public void delete(Long actorId, Long groupId) {
        Group group = groups.findById(groupId).orElseThrow(() -> ResourceNotFoundException.of("Group", groupId));
        if (groups.hasDependents(groupId)) {
            throw new ConflictException("GROUP_HAS_DEPENDENTS",
                    "The group has enrolled students or teaching assignments and cannot be deleted");
        }
        groups.deleteById(groupId);
        audit.success(actorId, AuditAction.DELETE, "Group", groupId, group.getName());
    }

    @Override
    public GroupView get(Long groupId) {
        return groups.findViewById(groupId).orElseThrow(() -> ResourceNotFoundException.of("Group", groupId));
    }

    @Override
    public PageResult<GroupView> search(Long gradeId, Integer academicYear, PageQuery page) {
        return groups.search(gradeId, academicYear, page);
    }

    private void requireUnique(Long gradeId, String name, int year, Long currentId) {
        groups.findByGradeIdAndNameAndAcademicYear(gradeId, name, year).filter(g -> !g.getId().equals(currentId))
                .ifPresent(g -> {
                    throw new ConflictException("GROUP_ALREADY_EXISTS",
                            "The group '" + name + "' already exists for that grade and academic year");
                });
    }
}
