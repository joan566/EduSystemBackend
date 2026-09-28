package com.edusistem.core.exam.domain.inputports;

import java.time.LocalDateTime;

/** Libera espacio: borra el PDF original de los lotes terminados antes de {@code cutoff}. */
public interface PurgeSubmissionBatchFilesUseCase {

    /** Devuelve cuántos archivos se borraron. Los resultados de los lotes (páginas, notas) no se tocan. */
    int purgeFilesCompletedBefore(LocalDateTime cutoff);
}
