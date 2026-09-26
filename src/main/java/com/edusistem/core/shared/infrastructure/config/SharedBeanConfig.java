package com.edusistem.core.shared.infrastructure.config;

import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.outputports.OwnershipPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo shared. */
@Configuration
public class SharedBeanConfig {

    @Bean
    OwnershipGuard ownershipGuard(OwnershipPort ownership) {
        return new OwnershipGuard(ownership);
    }
}
