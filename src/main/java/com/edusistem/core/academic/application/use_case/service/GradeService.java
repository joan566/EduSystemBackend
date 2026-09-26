package com.edusistem.core.academic.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.Grade;
import com.edusistem.core.academic.domain.inputports.ManageGradeUseCase;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.CatalogUsagePort;
import java.util.List;

public class GradeService implements ManageGradeUseCase {

    private final GradeRepositoryPort grades;
    private final CatalogUsagePort usage;
    private final RecordAuditUseCase audit;

    public GradeService(GradeRepositoryPort grades, CatalogUsagePort usage, RecordAuditUseCase audit) {
        this.grades = grades;
        this.usage = usage;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public Grade create(AcademicCommands.CreateGrade command) {
        String name = command.name().trim();
        requireNameAvailable(name, null);
        Grade saved = grades.save(Grade.builder().name(name).description(blankToNull(command.description())).build());
        audit.success(command.actorId(), AuditAction.CREATE, "Grade", saved.getId(), name);
        return saved;
    }

    @Override
    @UseCaseTransactional
    public Grade update(AcademicCommands.UpdateGrade command) {
        Grade grade = get(command.gradeId());
        if (usage.gradeUsedByOtherTeachers(grade.getId(), command.actorId())) {
            throw new ConflictException("CATALOG_ITEM_IN_USE", "The grade is used by other teachers and cannot be modified");
        }
        String name = command.name().trim();
        requireNameAvailable(name, grade.getId());
        grade.setName(name);
        grade.setDescription(blankToNull(command.description()));
        Grade saved = grades.save(grade);
        audit.success(command.actorId(), AuditAction.UPDATE, "Grade", saved.getId(), name);
        return saved;
    }

    @Override
    @UseCaseTransactional
    public void delete(Long actorId, Long gradeId) {
        Grade grade = get(gradeId);
        if (grades.hasGroups(gradeId)) {
            throw new ConflictException("GRADE_HAS_GROUPS", "The grade has groups and cannot be deleted");
        }
        grades.deleteById(gradeId);
        audit.success(actorId, AuditAction.DELETE, "Grade", gradeId, grade.getName());
    }

    @Override
    public Grade get(Long gradeId) {
        return grades.findById(gradeId).orElseThrow(() -> ResourceNotFoundException.of("Grade", gradeId));
    }

    @Override
    public List<Grade> list() {
        return grades.findAllOrderedByName();
    }

    private void requireNameAvailable(String name, Long currentId) {
        grades.findByName(name).filter(g -> !g.getId().equals(currentId)).ifPresent(g -> {
            throw new ConflictException("GRADE_ALREADY_EXISTS", "A grade named '" + name + "' already exists");
        });
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
