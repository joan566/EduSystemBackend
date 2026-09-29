package com.edusistem.core.audit.application.use_case.service;

import com.edusistem.core.audit.application.use_case.dtos.AuditLogFilter;
import com.edusistem.core.audit.domain.entity.AuditLog;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.enums.AuditResult;
import com.edusistem.core.audit.domain.inputports.ListAuditLogsUseCase;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.audit.domain.outputports.AuditLogRepositoryPort;
import com.edusistem.core.audit.domain.vo.AuditTarget;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditService implements RecordAuditUseCase, ListAuditLogsUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int MAX_DETAILS = 4000;
    private static final int MAX_LABEL = 255;

    private final AuditLogRepositoryPort repository;
    private final Clock clock;

    public AuditService(AuditLogRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void success(Long userId, AuditAction action, AuditTarget target, String details) {
        repository.save(build(userId, action, target, AuditResult.SUCCESS, details));
    }

    @Override
    public void failure(Long userId, AuditAction action, AuditTarget target, String details) {
        try {
            repository.saveIndependently(build(userId, action, target, AuditResult.FAILURE, details));
        } catch (RuntimeException e) {
            // Un fallo de auditoría nunca debe ocultar el error original.
            log.error("Could not persist failure audit for action {} on {}", action, target.entityType(), e);
        }
    }

    @Override
    public PageResult<AuditLog> list(Long userId, AuditLogFilter filter, PageQuery page) {
        return repository.findByUserId(userId, filter, page);
    }

    private AuditLog build(Long userId, AuditAction action, AuditTarget target, AuditResult result, String details) {
        return AuditLog.builder().userId(userId).action(action).entityType(target.entityType())
                .entityId(target.entityId()).teachingPeriodId(target.teachingPeriodId())
                .entityLabel(truncate(target.label(), MAX_LABEL)).result(result).details(truncate(details, MAX_DETAILS))
                .createdAt(LocalDateTime.now(clock)).build();
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
