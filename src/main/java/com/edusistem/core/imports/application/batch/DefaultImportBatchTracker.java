package com.edusistem.core.imports.application.batch;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.imports.application.contracts.ImportBatchTracker;
import com.edusistem.core.imports.application.contracts.ImportOutcome;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.vo.ImportBatchDetails;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No es transaccional a propósito: el batch debe poder quedar registrado como FAILED aunque la transacción de la
 * importación se revierta.
 */
public class DefaultImportBatchTracker implements ImportBatchTracker {

    private static final Logger log = LoggerFactory.getLogger(DefaultImportBatchTracker.class);
    /** Uno más de los que se devuelven, para saber si la lista está truncada; todos quedan en el reporte en Excel. */
    private static final int MAX_STORED_ERRORS = ImportBatchDetails.MAX_ERRORS + 1;

    private final ImportBatchRepositoryPort batches;
    private final FileStoragePort storage;
    private final SpreadsheetWriterPort writer;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    public DefaultImportBatchTracker(ImportBatchRepositoryPort batches, FileStoragePort storage,
                                     SpreadsheetWriterPort writer, RecordAuditUseCase audit, Clock clock) {
        this.batches = batches;
        this.storage = storage;
        this.writer = writer;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public void complete(ImportBatch batch, ImportOutcome outcome) {
        int total = outcome.totalRows();
        int successful = outcome.successfulRows();
        int failedRows = outcome.failedRows();
        List<ImportRowError> errors = outcome.errors();
        batch.setTotalRows(total);
        batch.setSuccessfulRows(successful);
        batch.setFailedRows(failedRows);
        batch.setStatus(failedRows == 0 ? ImportStatus.COMPLETED
                : successful > 0 ? ImportStatus.COMPLETED_WITH_ERRORS : ImportStatus.FAILED);
        batch.setCompletedAt(LocalDateTime.now(clock));
        if (!errors.isEmpty()) {
            List<List<Object>> lines = errors.stream()
                    .map(e -> List.<Object>of(e.rowNumber(), e.column() == null ? "" : e.column(), e.message())).toList();
            batch.setErrorReportPath(storage.store("imports/errors", "import-errors.xlsx",
                    writer.write(SpreadsheetVocabulary.ERRORS.table(List.of("row", "column", "error"), lines))));
            batches.saveErrors(batch.getId(), errors.subList(0, Math.min(errors.size(), MAX_STORED_ERRORS)));
        }
        ImportBatch saved = batches.save(batch);
        audit.success(batch.getUserId(), AuditAction.IMPORT, "ImportBatch", saved.getId(),
                "rows " + total + ", ok " + successful + ", failed " + failedRows);
    }

    @Override
    public void fail(ImportBatch batch, String code, String message) {
        log.warn("Import {} failed: {}", batch.getId(), message);
        batch.fail(code, message, LocalDateTime.now(clock));
        try {
            batches.save(batch);
        } catch (RuntimeException e) {
            log.error("Could not mark import batch {} as failed", batch.getId(), e);
        }
        audit.failure(batch.getUserId(), AuditAction.IMPORT, "ImportBatch", batch.getId(), message);
    }
}
