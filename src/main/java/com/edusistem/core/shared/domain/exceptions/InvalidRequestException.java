package com.edusistem.core.shared.domain.exceptions;

import java.util.List;

/** Datos de entrada semánticamente inválidos detectados en la capa de aplicación/dominio. */
public class InvalidRequestException extends DomainException {

    /** Problema concreto dentro de la petición (p. ej. una pregunta o un párrafo de un documento importado). */
    public record Detail(String field, String message) {
    }

    private final List<Detail> details;

    public InvalidRequestException(String code, String message) {
        this(code, message, List.of());
    }

    public InvalidRequestException(String code, String message, List<Detail> details) {
        super(code, message);
        this.details = List.copyOf(details);
    }

    public List<Detail> getDetails() {
        return details;
    }
}
