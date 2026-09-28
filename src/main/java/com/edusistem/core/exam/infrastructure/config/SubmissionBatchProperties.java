package com.edusistem.core.exam.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * edusistem.submission-batches.*: cuántos PDF se procesan a la vez en segundo plano y cuántos días se conserva el PDF
 * original de un lote terminado.
 */
@ConfigurationProperties(prefix = "edusistem.submission-batches")
public record SubmissionBatchProperties(Integer workers, Integer fileRetentionDays) {

    public int workersOrDefault() {
        return workers == null || workers < 1 ? 2 : workers;
    }

    public int fileRetentionDaysOrDefault() {
        return fileRetentionDays == null || fileRetentionDays < 0 ? 30 : fileRetentionDays;
    }
}
