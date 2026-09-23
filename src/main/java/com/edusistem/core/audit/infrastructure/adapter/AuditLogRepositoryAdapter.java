package com.edusistem.core.audit.infrastructure.adapter;

import com.edusistem.core.audit.application.use_case.dtos.AuditLogFilter;
import com.edusistem.core.audit.domain.entity.AuditLog;
import com.edusistem.core.audit.domain.outputports.AuditLogRepositoryPort;
import com.edusistem.core.audit.infrastructure.entity.AuditLogEntity;
import com.edusistem.core.audit.infrastructure.mapper.AuditLogMapper;
import com.edusistem.core.audit.infrastructure.repository.SpringDataAuditLogRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLogRepositoryAdapter implements AuditLogRepositoryPort {

    private final SpringDataAuditLogRepository repository;
    private final AuditLogMapper mapper;

    public AuditLogRepositoryAdapter(SpringDataAuditLogRepository repository, AuditLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AuditLog save(AuditLog log) {
        return mapper.toDomain(repository.save(mapper.toEntity(log)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog saveIndependently(AuditLog log) {
        return mapper.toDomain(repository.save(mapper.toEntity(log)));
    }

    @Override
    public PageResult<AuditLog> findByUserId(Long userId, AuditLogFilter filter, PageQuery page) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> cb.equal(root.get("userId"), userId);
        if (filter.action() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), filter.action()));
        }
        if (filter.entityType() != null && !filter.entityType().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), filter.entityType()));
        }
        if (filter.entityId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), filter.entityId()));
        }
        Page<AuditLogEntity> result = repository.findAll(spec,
                PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(), page.page(), page.size(),
                result.getTotalElements(), result.getTotalPages());
    }
}
