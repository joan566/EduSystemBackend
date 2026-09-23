package com.edusistem.core.audit.application.use_case.dtos;

import com.edusistem.core.audit.domain.enums.AuditAction;

public record AuditLogFilter(AuditAction action, String entityType, Long entityId) {
}
