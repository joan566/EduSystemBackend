package com.edusistem.core.imports.domain.vo;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import java.util.List;

/** Una importación con sus primeros errores por fila; el reporte completo se descarga en Excel. */
public record ImportBatchDetails(ImportBatch batch, List<ImportRowError> errors, boolean errorsTruncated) {

    /** Errores por fila que se devuelven con la importación. */
    public static final int MAX_ERRORS = 500;
}
