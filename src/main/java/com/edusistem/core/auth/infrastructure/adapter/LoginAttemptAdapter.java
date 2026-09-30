package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.outputports.LoginAttemptPort;
import com.edusistem.core.auth.infrastructure.security.LoginRateLimitProperties;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Ventana deslizante (en la base, común a todas las instancias): bloquea un correo/IP que acumula demasiados fallos
 * dentro de la ventana. Se usa la IP de la conexión: detrás de un proxy inverso hay que configurar
 * server.forward-headers-strategy.
 */
@Component
public class LoginAttemptAdapter implements LoginAttemptPort {

    private final SlidingWindowCounter failures;
    private final LoginRateLimitProperties properties;
    private final Duration window;

    public LoginAttemptAdapter(SlidingWindowCounter failures, LoginRateLimitProperties properties) {
        this.failures = failures;
        this.properties = properties;
        this.window = Duration.ofMinutes(properties.windowMinutes());
    }

    @Override
    public long retryAfterSeconds(String email, String clientIp) {
        return Math.max(failures.retryAfterSeconds("login:email:" + email, properties.maxAttemptsPerEmail(), window),
                failures.retryAfterSeconds("login:ip:" + clientIp, properties.maxAttemptsPerIp(), window));
    }

    @Override
    public void recordFailure(String email, String clientIp) {
        failures.record("login:email:" + email);
        failures.record("login:ip:" + clientIp);
    }

    @Override
    public void recordSuccess(String email) {
        failures.reset("login:email:" + email);
    }
}
