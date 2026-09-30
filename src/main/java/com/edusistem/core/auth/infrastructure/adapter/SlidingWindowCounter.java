package com.edusistem.core.auth.infrastructure.adapter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador en memoria de eventos por clave dentro de una ventana deslizante fija. Es por instancia (suficiente para un
 * monolito de un solo nodo; con varios nodos habría que moverlo a un almacén común).
 */
final class SlidingWindowCounter {

    private static final int PURGE_THRESHOLD = 10_000;

    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration window;

    SlidingWindowCounter(Clock clock, Duration window) {
        this.clock = clock;
        this.window = window;
    }

    /** Segundos que faltan para que la clave vuelva a estar por debajo de {@code max}; 0 si ya lo está. */
    long retryAfterSeconds(String key, int max) {
        Deque<Instant> timestamps = events.get(key);
        if (timestamps == null) {
            return 0;
        }
        synchronized (timestamps) {
            prune(timestamps);
            return retryAfter(timestamps, max);
        }
    }

    void record(String key) {
        Deque<Instant> timestamps = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            prune(timestamps);
            timestamps.addLast(clock.instant());
        }
        purgeIfLarge();
    }

    /** Comprueba y registra de forma atómica: solo registra si la clave está por debajo de {@code max}. */
    long tryRecord(String key, int max) {
        Deque<Instant> timestamps = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        long retryAfter;
        synchronized (timestamps) {
            prune(timestamps);
            retryAfter = retryAfter(timestamps, max);
            if (retryAfter == 0) {
                timestamps.addLast(clock.instant());
            }
        }
        purgeIfLarge();
        return retryAfter;
    }

    void reset(String key) {
        events.remove(key);
    }

    private long retryAfter(Deque<Instant> timestamps, int max) {
        if (timestamps.size() < max) {
            return 0;
        }
        long seconds = Duration.between(clock.instant(), timestamps.peekFirst().plus(window)).getSeconds();
        return Math.max(1, seconds + 1);
    }

    private void prune(Deque<Instant> timestamps) {
        Instant limit = clock.instant().minus(window);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(limit)) {
            timestamps.pollFirst();
        }
    }

    private void purgeIfLarge() {
        if (events.size() <= PURGE_THRESHOLD) {
            return;
        }
        events.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                prune(e.getValue());
                return e.getValue().isEmpty();
            }
        });
    }
}
