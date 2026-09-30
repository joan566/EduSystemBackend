package com.edusistem.core.imports.domain.inputports;

import java.time.LocalDateTime;

/** Borra el Excel subido y el informe de errores de las importaciones terminadas antes de {@code cutoff}. */
public interface PurgeImportFilesUseCase {

    /** Devuelve cuántas importaciones se limpiaron. El resumen (filas, estado) se conserva. */
    int purgeFilesCompletedBefore(LocalDateTime cutoff);
}
