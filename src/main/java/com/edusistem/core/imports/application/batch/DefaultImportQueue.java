package com.edusistem.core.imports.application.batch;

import com.edusistem.core.imports.application.contracts.ImportQueue;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.domain.inputports.ProcessImportBatchUseCase;
import com.edusistem.core.imports.domain.outputports.ImportBackgroundTaskPort;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Un profesor solo puede tener una importación en curso: las hojas "buscan o crean" grados, grupos, clases…, y dos
 * importaciones en paralelo podrían duplicarlos.
 */
public class DefaultImportQueue implements ImportQueue {

    private static final int MAX_FILE_NAME = 255;

    private final ImportBatchRepositoryPort batches;
    private final FileStoragePort storage;
    private final ImportBackgroundTaskPort background;
    private final ProcessImportBatchUseCase worker;
    private final Clock clock;

    public DefaultImportQueue(ImportBatchRepositoryPort batches, FileStoragePort storage,
                              ImportBackgroundTaskPort background, ProcessImportBatchUseCase worker, Clock clock) {
        this.batches = batches;
        this.storage = storage;
        this.background = background;
        this.worker = worker;
        this.clock = clock;
    }

    @Override
    public ImportBatch submit(ImportRequest request) {
        String name = request.fileName() == null ? request.defaultFileName() : request.fileName();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xlsx") || !looksLikeZip(request.content())) {
            throw new InvalidRequestException("INVALID_FILE_TYPE", ImportMessages.ONLY_XLSX);
        }
        if (batches.existsUnfinishedByUserId(request.teacherId())) {
            throw new ConflictException("IMPORT_IN_PROGRESS", ImportMessages.IMPORT_IN_PROGRESS);
        }
        ImportBatch batch = batches.save(ImportBatch.builder().userId(request.teacherId())
                .importType(request.type()).teachingPeriodId(request.teachingPeriodId())
                .fileName(name.length() > MAX_FILE_NAME ? name.substring(0, MAX_FILE_NAME) : name)
                .filePath(storage.store("imports", name, request.content()))
                .status(ImportStatus.QUEUED).createdAt(LocalDateTime.now(clock)).build());
        Long batchId = batch.getId();
        background.run(() -> worker.process(batchId));
        return batch;
    }

    private static boolean looksLikeZip(byte[] content) {
        return content != null && content.length > 4 && content[0] == 'P' && content[1] == 'K';
    }
}
