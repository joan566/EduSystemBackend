package com.edusistem.core.appversion.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Versiones de la app móvil en formato X.Y.Z (APP_LATEST_VERSION / APP_MINIMUM_VERSION). */
@ConfigurationProperties(prefix = "edusistem.app-version")
public record AppVersionProperties(String latest, String minimum) {
}
