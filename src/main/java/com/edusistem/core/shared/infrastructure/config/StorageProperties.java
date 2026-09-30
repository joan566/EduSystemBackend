package com.edusistem.core.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param encryptionKey clave AES-256 en Base64 (32 bytes) para cifrar los archivos guardados; vacía = sin cifrar
 */
@ConfigurationProperties(prefix = "edusistem.storage")
public record StorageProperties(String basePath, String encryptionKey) {
}
