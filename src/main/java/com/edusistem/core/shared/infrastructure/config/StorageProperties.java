package com.edusistem.core.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edusistem.storage")
public record StorageProperties(String basePath) {
}
