package com.edusistem.core.imports.domain.inputports;

import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.vo.ImportResult;

public interface ImportTeachingPeriodDataUseCase {

    /**
     * Aplica el Excel combinado de un teaching period (hojas Students/Grades/Attendance, todas opcionales): crea o
     * actualiza estudiantes y los matricula en el grupo del periodo, registra notas de actividades y asistencia.
     * Descargar {@code GET /exports/teaching-periods/{id}/full} produce un archivo listo para editar y reimportar.
     */
    ImportResult importData(ImportCommands.ImportTeachingPeriodData command);
}
