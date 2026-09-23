package com.edusistem.core.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edusistem.security.jwt")
public record JwtProperties(String secret, long expirationSeconds, long refreshExpirationDays) {
}
