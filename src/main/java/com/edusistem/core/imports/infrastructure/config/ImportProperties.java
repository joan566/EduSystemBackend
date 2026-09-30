package com.edusistem.core.imports.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** edusistem.imports.*: días que se conservan el Excel subido y el informe de errores de una importación. */
@ConfigurationProperties(prefix = "edusistem.imports")
public record ImportProperties(Integer fileRetentionDays) {

    public int fileRetentionDaysOrDefault() {
        return fileRetentionDays == null || fileRetentionDays < 0 ? 30 : fileRetentionDays;
    }
}
