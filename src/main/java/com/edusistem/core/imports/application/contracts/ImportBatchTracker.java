package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.domain.entity.ImportBatch;

/**
 * Deja registrado cómo terminó una importación en {@code import_batches}: estado, conteos, primeros errores, reporte de
 * errores en Excel y auditoría.
 */
public interface ImportBatchTracker {

    void complete(ImportBatch batch, ImportOutcome outcome);

    /** El archivo entero no se pudo importar; {@code code} es el código estable expuesto al cliente. */
    void fail(ImportBatch batch, String code, String message);
}
