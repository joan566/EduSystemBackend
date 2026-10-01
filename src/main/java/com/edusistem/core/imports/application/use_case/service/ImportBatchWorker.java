package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.imports.application.contracts.ImportBatchTracker;
import com.edusistem.core.imports.application.contracts.ImportProcessor;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;
import com.edusistem.core.imports.domain.inputports.ProcessImportBatchUseCase;
import com.edusistem.core.imports.domain.outputports.ImportBackgroundTaskPort;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.shared.domain.exceptions.DomainException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importa en segundo plano el Excel de un batch con el {@link ImportProcessor} de su tipo y deja el resultado con el
 * {@link ImportBatchTracker}. Las importaciones "buscan o crean" y actualizan, así que repetir una que un reinicio dejó
 * a medias es seguro.
 */
public class ImportBatchWorker implements ProcessImportBatchUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportBatchWorker.class);

    private final ImportBatchRepositoryPort batches;
    private final FileStoragePort storage;
    private final ImportBatchTracker tracker;
    private final ImportBackgroundTaskPort background;
    private final Map<ImportType, ImportProcessor> processors = new EnumMap<>(ImportType.class);
    private final Clock clock;

    public ImportBatchWorker(ImportBatchRepositoryPort batches, FileStoragePort storage, ImportBatchTracker tracker,
                             ImportBackgroundTaskPort background, List<ImportProcessor> processors, Clock clock) {
        this.batches = batches;
        this.storage = storage;
        this.tracker = tracker;
        this.background = background;
        this.clock = clock;
        for (ImportProcessor processor : processors) {
            if (this.processors.put(processor.type(), processor) != null) {
                throw new IllegalStateException("Duplicated import processor for " + processor.type());
            }
        }
    }

    @Override
    public void resumeUnfinished() {
        for (ImportBatch batch : batches.findUnfinished()) {
            log.info("Resuming import batch {}", batch.getId());
            background.run(() -> process(batch.getId()));
        }
    }

    @Override
    public void process(Long batchId) {
        ImportBatch batch = batches.findById(batchId).orElse(null);
        if (batch == null || batch.isFinished()) {
            return;
        }
        batch.start(LocalDateTime.now(clock));
        batch = batches.save(batch);
        try {
            ImportProcessor processor = processors.get(batch.getImportType());
            if (processor == null) {
                throw new IllegalStateException("No import processor for " + batch.getImportType());
            }
            byte[] content = storage.read(batch.getFilePath());
            tracker.complete(batch, processor.process(batch, content));
        } catch (DomainException e) {
            tracker.fail(batch, e.getCode(), e.getMessage());
        } catch (IOException e) {
            log.error("Could not read the stored file of import batch {}", batchId, e);
            tracker.fail(batch, "FILE_UNREADABLE", ImportMessages.FILE_UNREADABLE);
        } catch (RuntimeException e) {
            log.error("Unexpected error processing import batch {}", batchId, e);
            tracker.fail(batch, "PROCESSING_ERROR", ImportMessages.UNEXPECTED_ERROR);
        }
    }
}
