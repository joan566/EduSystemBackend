package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.domain.vo.ImportResult;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import java.util.List;

/**
 * Ciclo de vida de una importación en {@code import_batches}: valida que el archivo sea un .xlsx, lo guarda, registra el
 * batch en PROCESSING, ejecuta la importación y deja el resultado (estado, conteos, reporte de errores en Excel y
 * auditoría). Si la importación lanza una excepción, el batch queda FAILED y la excepción se propaga.
 */
public interface ImportBatchTracker {

    ImportResult track(Long teacherId, String fileName, String defaultFileName, byte[] content, ImportJob job);

    /** El trabajo propiamente dicho; se ejecuta con el batch ya registrado. */
    @FunctionalInterface
    interface ImportJob {
        ImportOutcome run();
    }

    /** Resultado de procesar el archivo: filas leídas, correctas, fallidas y el detalle de errores. */
    record ImportOutcome(int totalRows, int successfulRows, int failedRows, List<ImportRowError> errors) {
    }
}
