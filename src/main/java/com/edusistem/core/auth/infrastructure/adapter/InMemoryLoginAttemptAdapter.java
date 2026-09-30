package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.outputports.LoginAttemptPort;
import com.edusistem.core.auth.infrastructure.security.LoginRateLimitProperties;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Ventana deslizante en memoria: bloquea un correo/IP que acumula demasiados fallos dentro de la ventana.
 * Se usa la IP de la conexión: detrás de un proxy inverso hay que configurar server.forward-headers-strategy.
 */
@Component
public class InMemoryLoginAttemptAdapter implements LoginAttemptPort {

    private final SlidingWindowCounter failures;
    private final LoginRateLimitProperties properties;

    public InMemoryLoginAttemptAdapter(LoginRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.failures = new SlidingWindowCounter(clock, Duration.ofMinutes(properties.windowMinutes()));
    }

    @Override
    public long retryAfterSeconds(String email, String clientIp) {
        return Math.max(failures.retryAfterSeconds("email:" + email, properties.maxAttemptsPerEmail()),
                failures.retryAfterSeconds("ip:" + clientIp, properties.maxAttemptsPerIp()));
    }

    @Override
    public void recordFailure(String email, String clientIp) {
        failures.record("email:" + email);
        failures.record("ip:" + clientIp);
    }

    @Override
    public void recordSuccess(String email) {
        failures.reset("email:" + email);
    }
}
