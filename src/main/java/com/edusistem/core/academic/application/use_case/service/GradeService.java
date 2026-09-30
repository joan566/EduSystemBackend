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
import java.util.List;

/** Grados propios del profesor; los de otros profesores responden 404. */
public class GradeService implements ManageGradeUseCase {

    private final GradeRepositoryPort grades;
    private final RecordAuditUseCase audit;

    public GradeService(GradeRepositoryPort grades, RecordAuditUseCase audit) {
        this.grades = grades;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public Grade create(AcademicCommands.CreateGrade command) {
        String name = command.name().trim();
        requireNameAvailable(command.teacherId(), name, null);
        Grade saved = grades.save(Grade.builder().teacherId(command.teacherId()).name(name)
                .description(blankToNull(command.description())).build());
        audit.success(command.teacherId(), AuditAction.CREATE, "Grade", saved.getId(), name);
        return saved;
    }

    @Override
    @UseCaseTransactional
    public Grade update(AcademicCommands.UpdateGrade command) {
        Grade grade = get(command.teacherId(), command.gradeId());
        String name = command.name().trim();
        requireNameAvailable(command.teacherId(), name, grade.getId());
        grade.setName(name);
        grade.setDescription(blankToNull(command.description()));
        Grade saved = grades.save(grade);
        audit.success(command.teacherId(), AuditAction.UPDATE, "Grade", saved.getId(), name);
        return saved;
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long gradeId) {
        Grade grade = get(teacherId, gradeId);
        if (grades.hasGroups(gradeId)) {
            throw new ConflictException("GRADE_HAS_GROUPS", "The grade has groups and cannot be deleted");
        }
        grades.deleteById(gradeId);
        audit.success(teacherId, AuditAction.DELETE, "Grade", gradeId, grade.getName());
    }

    @Override
    public Grade get(Long teacherId, Long gradeId) {
        return grades.findById(gradeId).filter(g -> g.getTeacherId().equals(teacherId))
                .orElseThrow(() -> ResourceNotFoundException.of("Grade", gradeId));
    }

    @Override
    public List<Grade> list(Long teacherId) {
        return grades.findByTeacherIdOrderedByName(teacherId);
    }

    private void requireNameAvailable(Long teacherId, String name, Long currentId) {
        grades.findByTeacherIdAndName(teacherId, name).filter(g -> !g.getId().equals(currentId)).ifPresent(g -> {
            throw new ConflictException("GRADE_ALREADY_EXISTS", "A grade named '" + name + "' already exists");
        });
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
