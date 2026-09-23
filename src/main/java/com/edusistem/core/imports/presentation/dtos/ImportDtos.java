package com.edusistem.core.imports.presentation.dtos;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.vo.ImportResult;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import java.time.LocalDateTime;
import java.util.List;

public final class ImportDtos {

    private ImportDtos() {
    }

    public record ImportBatchResponse(Long id, String fileName, String status, Integer totalRows, Integer successfulRows,
                                      Integer failedRows, boolean hasErrorReport, LocalDateTime createdAt,
                                      LocalDateTime completedAt) {

        public static ImportBatchResponse from(ImportBatch b) {
            return new ImportBatchResponse(b.getId(), b.getFileName(), b.getStatus().name(), b.getTotalRows(),
                    b.getSuccessfulRows(), b.getFailedRows(), b.getErrorReportPath() != null, b.getCreatedAt(),
                    b.getCompletedAt());
        }
    }

    public record RowErrorResponse(int row, String column, String message) {

        static RowErrorResponse from(ImportRowError e) {
            return new RowErrorResponse(e.rowNumber(), e.column(), e.message());
        }
    }

    /** {@code totalRows}, {@code successfulRows}, {@code failedRows} y {@code errors}, como pide el contrato. */
    public record ImportResultResponse(Long id, String status, Integer totalRows, Integer successfulRows,
                                       Integer failedRows, List<RowErrorResponse> errors, boolean errorsTruncated) {

        public static ImportResultResponse from(ImportResult r) {
            ImportBatch b = r.batch();
            return new ImportResultResponse(b.getId(), b.getStatus().name(), b.getTotalRows(), b.getSuccessfulRows(),
                    b.getFailedRows(), r.errors().stream().map(RowErrorResponse::from).toList(), r.errorsTruncated());
        }
    }
}
