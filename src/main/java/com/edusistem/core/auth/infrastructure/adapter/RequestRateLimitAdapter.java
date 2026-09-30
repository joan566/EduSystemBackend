package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.outputports.RequestRateLimitPort;
import com.edusistem.core.auth.domain.vo.RateLimit;
import org.springframework.stereotype.Component;

/** Ventanas deslizantes en la base (comunes a todas las instancias). Cada clave incluye ya su propósito y su sujeto. */
@Component
public class RequestRateLimitAdapter implements RequestRateLimitPort {

    private final SlidingWindowCounter counter;

    public RequestRateLimitAdapter(SlidingWindowCounter counter) {
        this.counter = counter;
    }

    @Override
    public long tryConsume(String key, RateLimit limit) {
        return counter.tryRecord(key, limit.maxRequests(), limit.window());
    }
}
