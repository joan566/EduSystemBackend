package com.edusistem.core.shared.domain.exceptions;

/** Datos de entrada semánticamente inválidos detectados en la capa de aplicación/dominio. */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String code, String message) {
        super(code, message);
    }

}
