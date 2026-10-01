package com.edusistem.core.imports.application.batch;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.imports.application.contracts.ImportBatchTracker;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.vo.ImportResult;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No es transaccional a propósito: el batch debe poder quedar registrado como FAILED aunque la transacción de la
 * importación se revierta.
 */
public class DefaultImportBatchTracker implements ImportBatchTracker {

    private static final Logger log = LoggerFactory.getLogger(DefaultImportBatchTracker.class);
    private static final int MAX_RETURNED_ERRORS = 500;
    private static final int MAX_FILE_NAME = 255;

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
    public ImportResult track(Long teacherId, String fileName, String defaultFileName, byte[] content, ImportJob job) {
        String name = fileName == null ? defaultFileName : fileName;
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xlsx") || !looksLikeZip(content)) {
            throw new InvalidRequestException("INVALID_FILE_TYPE", ImportMessages.ONLY_XLSX);
        }
        ImportBatch batch = batches.save(ImportBatch.builder().userId(teacherId)
                .fileName(name.length() > MAX_FILE_NAME ? name.substring(0, MAX_FILE_NAME) : name)
                .filePath(storage.store("imports", name, content))
                .status(ImportStatus.PROCESSING).build());
        try {
            return finish(batch, teacherId, job.run());
        } catch (RuntimeException e) {
            fail(batch, teacherId, e);
            throw e;
        }
    }

    private ImportResult finish(ImportBatch batch, Long userId, ImportOutcome outcome) {
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
        }
        ImportBatch saved = batches.save(batch);
        audit.success(userId, AuditAction.IMPORT, "ImportBatch", saved.getId(),
                "rows " + total + ", ok " + successful + ", failed " + failedRows);
        boolean truncated = errors.size() > MAX_RETURNED_ERRORS;
        return new ImportResult(saved, truncated ? errors.subList(0, MAX_RETURNED_ERRORS) : errors, truncated);
    }

    private void fail(ImportBatch batch, Long userId, RuntimeException cause) {
        log.warn("Import {} failed: {}", batch.getId(), cause.getMessage());
        try {
            batch.setStatus(ImportStatus.FAILED);
            batch.setCompletedAt(LocalDateTime.now(clock));
            batches.save(batch);
        } catch (RuntimeException e) {
            log.error("Could not mark import batch {} as failed", batch.getId(), e);
        }
        audit.failure(userId, AuditAction.IMPORT, "ImportBatch", batch.getId(), cause.getMessage());
    }

    private static boolean looksLikeZip(byte[] content) {
        return content != null && content.length > 4 && content[0] == 'P' && content[1] == 'K';
    }
}
