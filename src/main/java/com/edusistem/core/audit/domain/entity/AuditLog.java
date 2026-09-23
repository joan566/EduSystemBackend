package com.edusistem.core.audit.domain.entity;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.enums.AuditResult;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private Long id;
    private Long userId;
    private AuditAction action;
    private String entityType;
    private Long entityId;
    private AuditResult result;
    private String details;
    private LocalDateTime createdAt;
}
