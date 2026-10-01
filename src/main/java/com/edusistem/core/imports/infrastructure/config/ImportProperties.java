package com.edusistem.core.imports.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * edusistem.imports.*: cuántas importaciones se procesan a la vez en segundo plano y días que se conservan el Excel
 * subido y el informe de errores de una importación.
 */
@ConfigurationProperties(prefix = "edusistem.imports")
public record ImportProperties(Integer workers, Integer fileRetentionDays) {

    public int workersOrDefault() {
        return workers == null || workers < 1 ? 2 : workers;
    }

    public int fileRetentionDaysOrDefault() {
        return fileRetentionDays == null || fileRetentionDays < 0 ? 30 : fileRetentionDays;
    }
}
