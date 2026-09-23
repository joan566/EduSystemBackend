package com.edusistem.core.shared.domain.exceptions;

/** Credenciales o token inválidos. */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }

}
