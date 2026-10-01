package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.inputports.QueryImportUseCase;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.vo.ImportBatchDetails;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class ImportQueryService implements QueryImportUseCase {

    private final ImportBatchRepositoryPort batches;
    private final FileStoragePort storage;

    public ImportQueryService(ImportBatchRepositoryPort batches, FileStoragePort storage) {
        this.batches = batches;
        this.storage = storage;
    }

    @Override
    public PageResult<ImportBatch> list(Long teacherId, PageQuery page) {
        return batches.findByUserId(teacherId, page);
    }

    @Override
    public ImportBatchDetails get(Long teacherId, Long batchId) {
        ImportBatch batch = find(teacherId, batchId);
        List<ImportRowError> errors = batches.findErrors(batchId);
        boolean truncated = errors.size() > ImportBatchDetails.MAX_ERRORS;
        return new ImportBatchDetails(batch, truncated ? errors.subList(0, ImportBatchDetails.MAX_ERRORS) : errors,
                truncated);
    }

    @Override
    public byte[] errorReport(Long teacherId, Long batchId) {
        ImportBatch batch = find(teacherId, batchId);
        if (batch.getErrorReportPath() == null || !storage.exists(batch.getErrorReportPath())) {
            throw new ResourceNotFoundException("ERROR_REPORT_NOT_FOUND", "This import has no error report");
        }
        try {
            return storage.read(batch.getErrorReportPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ImportBatch find(Long teacherId, Long batchId) {
        return batches.findById(batchId).filter(b -> b.getUserId().equals(teacherId))
                .orElseThrow(() -> ResourceNotFoundException.of("ImportBatch", batchId));
    }
}
