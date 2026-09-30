package com.edusistem.core.audit.infrastructure.adapter;

import com.edusistem.core.audit.infrastructure.config.AuditProperties;
import com.edusistem.core.audit.infrastructure.repository.SpringDataAuditLogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Limpieza diaria de la auditoría según edusistem.audit.retention-days y anonymous-retention-days. */
@Component
public class AuditLogPurger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPurger.class);

    private final SpringDataAuditLogRepository repository;
    private final AuditProperties properties;
    private final Clock clock;

    public AuditLogPurger(SpringDataAuditLogRepository repository, AuditProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "0 45 3 * * *")
    @Transactional
    public void purge() {
        LocalDateTime now = LocalDateTime.now(clock);
        int deleted = repository.deleteCreatedBefore(now.minusDays(properties.retentionDays()))
                + repository.deleteAnonymousCreatedBefore(now.minusDays(properties.anonymousRetentionDays()));
        if (deleted > 0) {
            log.info("Purged {} audit log entries past their retention period", deleted);
        }
    }
}
