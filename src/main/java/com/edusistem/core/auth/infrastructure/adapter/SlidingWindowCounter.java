package com.edusistem.core.auth.infrastructure.adapter;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Contador de eventos por clave dentro de una ventana deslizante, guardado en {@code rate_limit_events} para que sea
 * común a todas las instancias y sobreviva a reinicios. Cada operación va en su propia transacción: un intento fallido
 * debe contar aunque la petición que lo registra termine en error y haga rollback.
 */
@Component
class SlidingWindowCounter {

    /** Mayor ventana admitida; los eventos más antiguos se purgan. */
    static final Duration MAX_WINDOW = Duration.ofDays(2);
    /** Espacio propio de pg_advisory_xact_lock para no chocar con otros bloqueos consultivos. */
    private static final int LOCK_NAMESPACE = 0x45445553;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    SlidingWindowCounter(JdbcTemplate jdbc, PlatformTransactionManager transactionManager, Clock clock) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /** Segundos que faltan para que la clave vuelva a estar por debajo de {@code max}; 0 si ya lo está. */
    long retryAfterSeconds(String key, int max, Duration window) {
        return tx.execute(status -> retryAfter(key, max, window, now()));
    }

    void record(String key) {
        tx.executeWithoutResult(status -> insert(key, now()));
    }

    /** Comprueba y registra de forma atómica (bloqueo por clave): solo registra si está por debajo de {@code max}. */
    long tryRecord(String key, int max, Duration window) {
        return tx.execute(status -> {
            jdbc.query("select pg_advisory_xact_lock(?, hashtext(?))", rs -> null, LOCK_NAMESPACE, key);
            LocalDateTime now = now();
            long retryAfter = retryAfter(key, max, window, now);
            if (retryAfter == 0) {
                insert(key, now);
            }
            return retryAfter;
        });
    }

    void reset(String key) {
        tx.executeWithoutResult(status -> jdbc.update("delete from rate_limit_events where bucket = ?", key));
    }

    @Scheduled(cron = "0 15 * * * *")
    public void purge() {
        tx.executeWithoutResult(status -> jdbc.update("delete from rate_limit_events where occurred_at < ?",
                now().minus(MAX_WINDOW)));
    }

    private long retryAfter(String key, int max, Duration window, LocalDateTime now) {
        if (window.compareTo(MAX_WINDOW) > 0) {
            throw new IllegalArgumentException("Rate limit windows longer than " + MAX_WINDOW + " are not supported");
        }
        Window current = jdbc.queryForObject("select count(*) as total, min(occurred_at) as oldest "
                        + "from rate_limit_events where bucket = ? and occurred_at > ?",
                (rs, n) -> new Window(rs.getLong("total"), rs.getObject("oldest", LocalDateTime.class)),
                key, now.minus(window));
        if (current.total() < max) {
            return 0;
        }
        LocalDateTime oldest = current.oldest();
        long seconds = Duration.between(now, oldest.plus(window)).getSeconds();
        return Math.max(1, seconds + 1);
    }

    private void insert(String key, LocalDateTime now) {
        jdbc.update("insert into rate_limit_events (bucket, occurred_at) values (?, ?)", key, now);
    }

    private record Window(long total, LocalDateTime oldest) {
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
