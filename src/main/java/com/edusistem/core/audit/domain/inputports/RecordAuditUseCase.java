package com.edusistem.core.audit.domain.inputports;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.vo.AuditTarget;

public interface RecordAuditUseCase {

    /** Registra una acción exitosa; participa de la transacción de negocio en curso. */
    void success(Long userId, AuditAction action, AuditTarget target, String details);

    /** Registra un fallo en una transacción independiente, de modo que sobreviva a un rollback del negocio. */
    void failure(Long userId, AuditAction action, AuditTarget target, String details);

    default void success(Long userId, AuditAction action, String entityType, Long entityId, String details) {
        success(userId, action, AuditTarget.of(entityType, entityId), details);
    }

    default void failure(Long userId, AuditAction action, String entityType, Long entityId, String details) {
        failure(userId, action, AuditTarget.of(entityType, entityId), details);
    }
}
