package com.edusistem.core.auth.infrastructure.security;

import com.edusistem.core.auth.domain.vo.RateLimit;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * edusistem.security.rate-limits.*: límites de los endpoints públicos sensibles. Se aplican exista o no el correo,
 * para no revelar qué cuentas están registradas.
 */
@ConfigurationProperties(prefix = "edusistem.security.rate-limits")
public record RateLimitProperties(Limit registerPerIp, Limit forgotPasswordPerEmail, Limit forgotPasswordPerIp,
                                  Limit resetCodePerEmail, Limit resetCodePerIp) {

    public record Limit(int max, int windowMinutes) {

        public RateLimit toRateLimit() {
            return RateLimit.of(max, windowMinutes);
        }
    }
}
