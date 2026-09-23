package com.edusistem.core.student.domain.inputports;

import com.edusistem.core.student.application.use_case.dtos.StudentCommands;
import com.edusistem.core.student.domain.entity.Student;

/** Lógica de dominio de altas/modificaciones; la usa la importación de Excel (no hay CRUD expuesto por HTTP). */
public interface RegisterStudentUseCase {

    Student create(StudentCommands.Create command);

    Student update(StudentCommands.Update command);

    /** Matricula al estudiante en el grupo; reactiva la matrícula si estaba retirado. */
    void enroll(StudentCommands.Enroll command);
}
