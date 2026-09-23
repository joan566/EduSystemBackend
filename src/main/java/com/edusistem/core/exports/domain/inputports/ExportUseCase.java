package com.edusistem.core.exports.domain.inputports;

import com.edusistem.core.exports.domain.vo.ExportedFile;

public interface ExportUseCase {

    /** Estudiantes del teaching period o del grupo indicado; sin filtros, todos los del profesor. */
    ExportedFile students(Long teacherId, Long groupId, Long teachingPeriodId);

    /** Notas de cada evaluación del teaching period y nota del periodo (si la configuración suma 100%). */
    ExportedFile grades(Long teacherId, Long teachingPeriodId);

    /** Matriz estudiantes × sesiones de asistencia con totales. */
    ExportedFile attendance(Long teacherId, Long teachingPeriodId);

    /**
     * Un solo .xlsx con 3 hojas (Students, Grades, Attendance) del teaching period: estudiantes matriculados, notas de
     * actividades y asistencia. También sirve como plantilla para {@code POST /imports/teaching-periods/{id}}.
     */
    ExportedFile full(Long teacherId, Long teachingPeriodId);
}
