package com.edusistem.core.auth.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edusistem.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public boolean enabled() {
        return allowedOrigins != null && allowedOrigins.stream().anyMatch(o -> !o.isBlank());
    }
}
