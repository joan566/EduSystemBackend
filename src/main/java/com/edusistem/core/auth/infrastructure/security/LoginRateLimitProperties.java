package com.edusistem.core.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edusistem.security.login-rate-limit")
public record LoginRateLimitProperties(int maxAttemptsPerEmail, int maxAttemptsPerIp, int windowMinutes) {
}
