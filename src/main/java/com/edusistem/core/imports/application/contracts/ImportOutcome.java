package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.domain.vo.ImportRowError;
import java.util.List;

/** Resultado de procesar el archivo: filas leídas, correctas, fallidas y el detalle de errores. */
public record ImportOutcome(int totalRows, int successfulRows, int failedRows, List<ImportRowError> errors) {
}
