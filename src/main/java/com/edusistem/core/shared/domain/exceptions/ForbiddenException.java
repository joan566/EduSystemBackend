package com.edusistem.core.shared.domain.exceptions;

/** El usuario autenticado no puede realizar la operación. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String code, String message) {
        super(code, message);
    }

}
