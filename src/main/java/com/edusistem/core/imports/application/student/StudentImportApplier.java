package com.edusistem.core.imports.application.student;

import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.student.application.use_case.dtos.StudentCommands;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.inputports.RegisterStudentUseCase;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // un estudiante nuevo puede venir en varias filas (una por grupo): se crea con la primera
        Map<String, Long> createdByIdentification = new HashMap<>();
        for (StudentImportRow row : rows) {
            Long studentId = row.existingStudentId();
            if (studentId == null && row.identificationNumber() != null) {
                studentId = createdByIdentification.get(row.identificationNumber());
            }
            if (studentId == null) {
                Student created = students.create(new StudentCommands.Create(teacherId, row.identificationNumber(),
                        row.studentCode(), row.firstName(), row.lastName(), row.email()));
                studentId = created.getId();
                if (row.identificationNumber() != null) {
                    createdByIdentification.put(row.identificationNumber(), studentId);
                }
            } else {
                students.update(new StudentCommands.Update(teacherId, studentId, row.firstName(), row.lastName(),
                        row.email()));
            }
            students.enroll(new StudentCommands.Enroll(teacherId, studentId, row.groupId()));
        }
    }
}
