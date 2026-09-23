package com.edusistem.core.exam.domain.exceptions;

import com.edusistem.core.shared.domain.exceptions.DomainException;

/** La imagen no pudo procesarse (hoja no detectada, ilegible...). */
public class AnswerSheetProcessingException extends DomainException {

    public AnswerSheetProcessingException(String code, String message) {
        super(code, message);
    }
}
