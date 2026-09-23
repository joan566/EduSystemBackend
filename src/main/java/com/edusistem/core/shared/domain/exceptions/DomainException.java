package com.edusistem.core.shared.domain.exceptions;

/** Base de todas las excepciones de negocio; {@code code} es el código estable expuesto al cliente. */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
