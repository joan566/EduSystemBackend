package com.edusistem.core.imports.domain.inputports;

/** Procesa en segundo plano las importaciones encoladas. */
public interface ProcessImportBatchUseCase {

    /** Importa el archivo del batch; no hace nada si ya terminó. */
    void process(Long batchId);

    /** Reencola las importaciones que un reinicio dejó en QUEUED/PROCESSING. */
    void resumeUnfinished();
}
