package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.outputports.LoginAttemptPort;
import com.edusistem.core.auth.infrastructure.security.LoginRateLimitProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Ventana deslizante en memoria: bloquea un correo/IP que acumula demasiados fallos dentro de la ventana.
 * Es por instancia (suficiente para un monolito de un solo nodo; con varios nodos habría que moverlo a un almacén común).
 * Se usa la IP de la conexión: detrás de un proxy inverso hay que configurar server.forward-headers-strategy.
 */
@Component
public class InMemoryLoginAttemptAdapter implements LoginAttemptPort {

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final LoginRateLimitProperties properties;
    private final Clock clock;

    public InMemoryLoginAttemptAdapter(LoginRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public long retryAfterSeconds(String email, String clientIp) {
        return Math.max(retryAfter("email:" + email, properties.maxAttemptsPerEmail()),
                retryAfter("ip:" + clientIp, properties.maxAttemptsPerIp()));
    }

    @Override
    public void recordFailure(String email, String clientIp) {
        add("email:" + email);
        add("ip:" + clientIp);
        if (failures.size() > 10_000) {
            purgeExpired();
        }
    }

    @Override
    public void recordSuccess(String email) {
        failures.remove("email:" + email);
    }

    private long retryAfter(String key, int max) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return 0;
        }
        synchronized (attempts) {
            prune(attempts);
            if (attempts.size() < max) {
                return 0;
            }
            long seconds = Duration.between(clock.instant(), attempts.peekFirst().plus(window())).getSeconds();
            return Math.max(1, seconds + 1);
        }
    }

    private void add(String key) {
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(clock.instant());
        }
    }

    private void prune(Deque<Instant> attempts) {
        Instant limit = clock.instant().minus(window());
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(limit)) {
            attempts.pollFirst();
        }
    }

    private void purgeExpired() {
        failures.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                prune(e.getValue());
                return e.getValue().isEmpty();
            }
        });
    }

    private Duration window() {
        return Duration.ofMinutes(properties.windowMinutes());
    }
}
