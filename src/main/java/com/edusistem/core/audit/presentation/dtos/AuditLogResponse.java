package com.edusistem.core.audit.presentation.dtos;

import com.edusistem.core.audit.domain.entity.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(Long id, String action, String entityType, Long entityId, String result,
                               String details, LocalDateTime createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getAction().name(), log.getEntityType(), log.getEntityId(),
                log.getResult().name(), log.getDetails(), log.getCreatedAt());
    }
}
