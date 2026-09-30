package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.inputports.PurgeImportFilesUseCase;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import java.io.IOException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Los Excel importados contienen datos personales de estudiantes: no se guardan más de lo necesario. */
public class ImportFileRetentionService implements PurgeImportFilesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportFileRetentionService.class);

    private final ImportBatchRepositoryPort batches;
    private final FileStoragePort storage;

    public ImportFileRetentionService(ImportBatchRepositoryPort batches, FileStoragePort storage) {
        this.batches = batches;
        this.storage = storage;
    }

    @Override
    public int purgeFilesCompletedBefore(LocalDateTime cutoff) {
        int purged = 0;
        for (ImportBatch batch : batches.findCompletedWithFilesBefore(cutoff)) {
            try {
                if (batch.getFilePath() != null) {
                    storage.delete(batch.getFilePath());
                }
                if (batch.getErrorReportPath() != null) {
                    storage.delete(batch.getErrorReportPath());
                }
            } catch (IOException e) { // se reintenta en la siguiente ejecución
                log.warn("Could not delete the files of import batch {}", batch.getId(), e);
                continue;
            }
            batch.setFilePath(null);
            batch.setErrorReportPath(null);
            batches.save(batch);
            purged++;
        }
        return purged;
    }
}
