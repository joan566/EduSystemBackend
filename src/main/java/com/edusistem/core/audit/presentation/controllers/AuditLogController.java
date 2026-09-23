package com.edusistem.core.audit.presentation.controllers;

import com.edusistem.core.audit.application.use_case.dtos.AuditLogFilter;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.ListAuditLogsUseCase;
import com.edusistem.core.audit.presentation.dtos.AuditLogResponse;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit")
public class AuditLogController {

    private final ListAuditLogsUseCase listAuditLogs;

    public AuditLogController(ListAuditLogsUseCase listAuditLogs) {
        this.listAuditLogs = listAuditLogs;
    }

    @GetMapping
    @Operation(summary = "Lista paginada de la auditoría del usuario autenticado")
    public PageResponse<AuditLogResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestParam(required = false) AuditAction action,
                                               @RequestParam(required = false) String entityType,
                                               @RequestParam(required = false) Long entityId,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        var result = listAuditLogs.list(user.id(), new AuditLogFilter(action, entityType, entityId),
                PageQuery.of(page, size));
        return PageResponse.from(result, AuditLogResponse::from);
    }
}
