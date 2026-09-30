package com.edusistem.core.audit.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param retentionDays          días que se conserva la auditoría de los profesores
 * @param anonymousRetentionDays días que se conservan las entradas sin usuario (intentos de login con correos
 *                               inexistentes, que guardan el correo escrito)
 */
@ConfigurationProperties(prefix = "edusistem.audit")
public record AuditProperties(int retentionDays, int anonymousRetentionDays) {
}
