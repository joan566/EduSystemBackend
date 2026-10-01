package com.edusistem.core.imports.presentation.dtos;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.vo.ImportBatchDetails;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import java.time.LocalDateTime;
import java.util.List;

public final class ImportDtos {

    private ImportDtos() {
    }

    /**
     * Resumen de una importación. {@code type} es nulo en las importaciones anteriores a la cola; {@code errorCode} y
     * {@code errorMessage} explican un FAILED del archivo entero (no por filas). Mientras corre, {@code totalRows} (fijado
     * al leer el archivo), {@code processedRows}, {@code progressPercent} y {@code currentStep} (la hoja en proceso)
     * muestran el avance; al terminar {@code progressPercent} es 100.
     */
    public record ImportBatchResponse(Long id, String type, String fileName, String status, Integer totalRows,
                                      Integer processedRows, int progressPercent, String currentStep,
                                      Integer successfulRows, Integer failedRows, boolean hasErrorReport,
                                      String errorCode, String errorMessage, LocalDateTime createdAt,
                                      LocalDateTime startedAt, LocalDateTime completedAt) {

        public static ImportBatchResponse from(ImportBatch b) {
            return new ImportBatchResponse(b.getId(), b.getImportType() == null ? null : b.getImportType().name(),
                    b.getFileName(), b.getStatus().name(), b.getTotalRows(), b.getProcessedRows(), b.progressPercent(),
                    b.getCurrentStep(), b.getSuccessfulRows(), b.getFailedRows(),
                    b.getErrorReportPath() != null, b.getErrorCode(), b.getErrorMessage(), b.getCreatedAt(),
                    b.getStartedAt(), b.getCompletedAt());
        }
    }

    public record RowErrorResponse(int row, String column, String message) {

        static RowErrorResponse from(ImportRowError e) {
            return new RowErrorResponse(e.rowNumber(), e.column(), e.message());
        }
    }

    /** El resumen más los primeros errores por fila; el detalle completo está en {@code /error-report}. */
    public record ImportBatchDetailsResponse(Long id, String type, String fileName, String status, Integer totalRows,
                                             Integer processedRows, int progressPercent, String currentStep,
                                             Integer successfulRows, Integer failedRows, boolean hasErrorReport,
                                             String errorCode, String errorMessage, LocalDateTime createdAt,
                                             LocalDateTime startedAt, LocalDateTime completedAt,
                                             List<RowErrorResponse> errors, boolean errorsTruncated) {

        public static ImportBatchDetailsResponse from(ImportBatchDetails d) {
            ImportBatchResponse b = ImportBatchResponse.from(d.batch());
            return new ImportBatchDetailsResponse(b.id(), b.type(), b.fileName(), b.status(), b.totalRows(),
                    b.processedRows(), b.progressPercent(), b.currentStep(), b.successfulRows(), b.failedRows(), b.hasErrorReport(), b.errorCode(), b.errorMessage(),
                    b.createdAt(), b.startedAt(), b.completedAt(),
                    d.errors().stream().map(RowErrorResponse::from).toList(), d.errorsTruncated());
        }
    }
}
