package com.edusistem.core.exam.domain.inputports;

/** Procesamiento en segundo plano de los lotes (lo invoca la infraestructura, no la API). */
public interface ProcessSubmissionBatchUseCase {

    /** Procesa las páginas pendientes del lote; no lanza excepciones: los errores quedan en el estado del lote. */
    void process(Long batchId);

    /** Vuelve a encolar los lotes que quedaron a medias (p. ej. tras un reinicio del servidor). */
    void resumeUnfinished();
}
