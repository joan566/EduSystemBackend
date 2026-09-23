package com.edusistem.core.shared.domain.exceptions;

/** El recurso no existe o no pertenece al usuario autenticado (se responde igual para no filtrar existencia). */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("RESOURCE_NOT_FOUND", resource + " not found: " + id);
    }
}
