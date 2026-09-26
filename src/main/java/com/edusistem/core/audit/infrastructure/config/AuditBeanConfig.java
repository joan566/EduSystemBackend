package com.edusistem.core.audit.infrastructure.config;

import com.edusistem.core.audit.application.use_case.service.AuditService;
import com.edusistem.core.audit.domain.outputports.AuditLogRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo audit. */
@Configuration
public class AuditBeanConfig {

    @Bean
    AuditService auditService(AuditLogRepositoryPort repository, Clock clock) {
        return new AuditService(repository, clock);
    }
}
