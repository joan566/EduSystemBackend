package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.infrastructure.repository.SpringDataRefreshTokenRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Limpieza diaria de refresh tokens vencidos hace más de 7 días. */
@Component
public class RefreshTokenPurger {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenPurger.class);

    private final SpringDataRefreshTokenRepository repository;
    private final Clock clock;

    public RefreshTokenPurger(SpringDataRefreshTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purge() {
        int deleted = repository.deleteExpiredBefore(LocalDateTime.now(clock).minusDays(7));
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        }
    }
}
