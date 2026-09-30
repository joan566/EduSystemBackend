package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.student.application.use_case.dtos.StudentCommands;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.inputports.RegisterStudentUseCase;
import java.util.List;

/**
 * Aplica en UNA transacción las filas ya validadas (crear/actualizar estudiantes + matricular en el grupo).
 * Si algo inesperado falla, no queda ninguna escritura parcial.
 */
public class StudentImportApplier {

    private final RegisterStudentUseCase students;

    public StudentImportApplier(RegisterStudentUseCase students) {
        this.students = students;
    }

    @UseCaseTransactional
    public void apply(Long teacherId, List<StudentImportRow> rows) {
        for (StudentImportRow row : rows) {
            Long studentId = row.existingStudentId();
            if (studentId == null) {
                Student created = students.create(new StudentCommands.Create(teacherId, row.identificationNumber(),
                        row.studentCode(), row.firstName(), row.lastName(), row.email()));
                studentId = created.getId();
            } else {
                students.update(new StudentCommands.Update(teacherId, studentId, row.firstName(), row.lastName(),
                        row.email()));
            }
            students.enroll(new StudentCommands.Enroll(teacherId, studentId, row.groupId()));
        }
    }
}
