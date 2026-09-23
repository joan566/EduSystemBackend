package com.edusistem.core.audit.domain.inputports;

import com.edusistem.core.audit.domain.enums.AuditAction;

public interface RecordAuditUseCase {

    /** Registra una acción exitosa; participa de la transacción de negocio en curso. */
    void success(Long userId, AuditAction action, String entityType, Long entityId, String details);

    /** Registra un fallo en una transacción independiente, de modo que sobreviva a un rollback del negocio. */
    void failure(Long userId, AuditAction action, String entityType, Long entityId, String details);
}
