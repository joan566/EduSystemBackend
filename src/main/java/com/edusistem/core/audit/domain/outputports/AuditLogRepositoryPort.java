package com.edusistem.core.audit.domain.outputports;

import com.edusistem.core.audit.application.use_case.dtos.AuditLogFilter;
import com.edusistem.core.audit.domain.entity.AuditLog;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface AuditLogRepositoryPort {

    AuditLog save(AuditLog log);

    /** Persiste en una transacción propia (REQUIRES_NEW). */
    AuditLog saveIndependently(AuditLog log);

    PageResult<AuditLog> findByUserId(Long userId, AuditLogFilter filter, PageQuery page);
}
