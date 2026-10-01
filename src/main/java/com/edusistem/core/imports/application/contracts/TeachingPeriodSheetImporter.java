package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.application.teachingperiod.TeachingPeriodImportContext;

/**
 * Importa una hoja del libro completo de un teaching period (Estudiantes, Notas o Asistencia). El caso de uso las
 * ejecuta en el orden en que se le entregan: primero Estudiantes, para que Notas y Asistencia vean a los recién
 * matriculados.
 */
public interface TeachingPeriodSheetImporter {

    /**
     * Valida y aplica las filas de la hoja. Los errores por fila se agregan al contexto; una columna obligatoria que
     * falta lanza una excepción que aborta la importación.
     * @return cantidad de filas de la hoja (0 si el libro no la trae)
     */
    int importSheet(TeachingPeriodImportContext context);

    /** Filas que trae la hoja (0 si el libro no la trae), para el total del avance. */
    int rowCount(TeachingPeriodImportContext context);
}
