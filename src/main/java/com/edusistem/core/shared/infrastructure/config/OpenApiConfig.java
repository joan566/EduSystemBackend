package com.edusistem.core.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI edusistemOpenApi() {
        return new OpenAPI()
                .info(new Info().title("EduSistem API").version("v1")
                        .description("Backend REST para la gestión de evaluaciones de profesores. Autenticación: JWT Bearer. "
                                + "Todos los errores tienen la forma {timestamp, status, code, message, path[, errors]}; "
                                + "los recursos de otro profesor responden 404 y las reglas de negocio 409/422."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
