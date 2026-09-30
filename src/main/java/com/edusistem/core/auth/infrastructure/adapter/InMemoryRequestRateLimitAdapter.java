package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.outputports.RequestRateLimitPort;
import com.edusistem.core.auth.domain.vo.RateLimit;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Ventanas deslizantes en memoria (una por duración de ventana). Mismas limitaciones que el login: un solo nodo. */
@Component
public class InMemoryRequestRateLimitAdapter implements RequestRateLimitPort {

    private final Map<Duration, SlidingWindowCounter> counters = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryRequestRateLimitAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public long tryConsume(String key, RateLimit limit) {
        return counters.computeIfAbsent(limit.window(), w -> new SlidingWindowCounter(clock, w))
                .tryRecord(key, limit.maxRequests());
    }
}
