package com.edusistem.core.exam.domain.enums;

/** Resultado de una página de un PDF procesado en lote. */
public enum BatchPageOutcome {
    /** Calificada sin dudas. */
    PROCESSED,
    /** Calificada, pero con respuestas pendientes de revisión manual. */
    REVIEW_REQUIRED,
    /** Estudiante identificado pero la hoja no pudo leerse; queda una submission FAILED consultable. */
    FAILED,
    /** No se guardó nada (QR ilegible/inválido, estudiante ajeno al grupo, duplicado...); ver el código de error. */
    REJECTED,
    /** La página no es una hoja de respuestas (sin QR ni marcadores), p. ej. el cuadernillo de preguntas. */
    SKIPPED
}
