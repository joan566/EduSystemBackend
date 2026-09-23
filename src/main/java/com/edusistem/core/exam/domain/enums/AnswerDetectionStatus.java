package com.edusistem.core.exam.domain.enums;

/** Resultado de la lectura de una pregunta; MANUAL indica que el profesor la fijó/confirmó. */
public enum AnswerDetectionStatus {
    MARKED, EMPTY, MULTIPLE_MARK, REVIEW_REQUIRED, MANUAL
}
