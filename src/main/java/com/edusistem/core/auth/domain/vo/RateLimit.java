package com.edusistem.core.auth.domain.vo;

import java.time.Duration;

/** Máximo de peticiones permitidas dentro de una ventana deslizante. */
public record RateLimit(int maxRequests, Duration window) {

    public static RateLimit of(int maxRequests, int windowMinutes) {
        return new RateLimit(maxRequests, Duration.ofMinutes(windowMinutes));
    }
}
