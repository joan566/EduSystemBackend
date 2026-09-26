package com.edusistem.core.user.infrastructure.config;

import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.user.application.use_case.service.UserProfileService;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo user. */
@Configuration
public class UserBeanConfig {

    @Bean
    UserProfileService userProfileService(UserRepositoryPort users, RecordAuditUseCase audit) {
        return new UserProfileService(users, audit);
    }
}
