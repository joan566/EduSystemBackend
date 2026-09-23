package com.edusistem.core.audit.domain.inputports;

import com.edusistem.core.audit.application.use_case.dtos.AuditLogFilter;
import com.edusistem.core.audit.domain.entity.AuditLog;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface ListAuditLogsUseCase {

    PageResult<AuditLog> list(Long userId, AuditLogFilter filter, PageQuery page);
}
