package com.edusistem.core.imports.domain.inputports;

import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.entity.ImportBatch;

public interface ImportTeachingPeriodDataUseCase {

    /**
     * Encola el Excel combinado de un teaching period (hojas Students/Grades/Attendance, todas opcionales): devuelve el
     * batch en QUEUED y en segundo plano crea o actualiza estudiantes y los matricula en el grupo del periodo, registra
     * notas de actividades y asistencia.
     * Descargar {@code GET /exports/teaching-periods/{id}/full} produce un archivo listo para editar y reimportar.
     */
    ImportBatch importData(ImportCommands.ImportTeachingPeriodData command);
}
