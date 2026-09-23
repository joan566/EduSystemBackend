package com.edusistem.core.audit.infrastructure.mapper;

import com.edusistem.core.audit.domain.entity.AuditLog;
import com.edusistem.core.audit.infrastructure.entity.AuditLogEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLog toDomain(AuditLogEntity entity);

    AuditLogEntity toEntity(AuditLog domain);
}
