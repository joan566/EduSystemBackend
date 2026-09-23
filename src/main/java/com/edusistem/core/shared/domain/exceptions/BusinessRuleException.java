package com.edusistem.core.shared.domain.exceptions;

/** Se violó una regla de negocio. */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }

}
