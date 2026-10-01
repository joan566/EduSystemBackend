package com.edusistem.core.imports.domain.outputports;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImportBatchRepositoryPort {

    ImportBatch save(ImportBatch batch);

    Optional<ImportBatch> findById(Long id);

    /**
     * Registra el avance de una importación en curso sin tocar el resto del batch. Se confirma de inmediato (aunque
     * quien lo llame esté dentro de otra transacción) para que el cliente lo vea mientras corre.
     */
    void updateProgress(Long id, int processedRows, int totalRows, String currentStep);

    PageResult<ImportBatch> findByUserId(Long userId, PageQuery page);

    /** Importaciones terminadas antes de {@code cutoff} que aún conservan el Excel o el informe de errores. */
    List<ImportBatch> findCompletedWithFilesBefore(LocalDateTime cutoff);

    /** Importaciones en cola o a medias (solo las encoladas; las síncronas antiguas se ignoran). */
    List<ImportBatch> findUnfinished();

    boolean existsUnfinishedByUserId(Long userId);

    void saveErrors(Long batchId, List<ImportRowError> errors);

    List<ImportRowError> findErrors(Long batchId);
}
