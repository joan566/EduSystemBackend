package com.edusistem.core.imports.domain.enums;

/** QUEUED: archivo recibido, a la espera de un worker; PROCESSING: importándose en segundo plano. */
public enum ImportStatus {
    QUEUED, PROCESSING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED
}
