package com.edusistem.core.shared.domain.exceptions;

/** La operación choca con el estado actual (duplicados, dependencias existentes). */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }

}
