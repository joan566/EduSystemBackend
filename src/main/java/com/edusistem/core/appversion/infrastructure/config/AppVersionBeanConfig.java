package com.edusistem.core.appversion.infrastructure.config;

import com.edusistem.core.appversion.application.use_case.service.AppVersionService;
import com.edusistem.core.appversion.domain.vo.AppVersion;
import com.edusistem.core.appversion.domain.vo.AppVersionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra el caso de uso de versión de la app; una configuración inválida impide arrancar. */
@Configuration
public class AppVersionBeanConfig {

    @Bean
    AppVersionService appVersionService(AppVersionProperties properties) {
        return new AppVersionService(new AppVersionPolicy(AppVersion.parse(properties.latest()),
                AppVersion.parse(properties.minimum())));
    }
}
