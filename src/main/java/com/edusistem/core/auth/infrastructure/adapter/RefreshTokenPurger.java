package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.infrastructure.repository.SpringDataPasswordResetTokenRepository;
import com.edusistem.core.auth.infrastructure.repository.SpringDataRefreshTokenRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Limpieza diaria de refresh tokens vencidos hace más de 7 días y de códigos (recuperación/verificación) vencidos. */
@Component
public class RefreshTokenPurger {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenPurger.class);

    private final SpringDataRefreshTokenRepository repository;
    private final SpringDataPasswordResetTokenRepository resetTokens;
    private final Clock clock;

    public RefreshTokenPurger(SpringDataRefreshTokenRepository repository,
                              SpringDataPasswordResetTokenRepository resetTokens, Clock clock) {
        this.repository = repository;
        this.resetTokens = resetTokens;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purge() {
        LocalDateTime now = LocalDateTime.now(clock);
        int deleted = repository.deleteExpiredBefore(now.minusDays(7));
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        }
        // Un código vencido ya no sirve para nada (findLatestByUserId solo mira el último)
        int codes = resetTokens.deleteExpiredBefore(now.minusDays(1));
        if (codes > 0) {
            log.info("Purged {} expired password reset codes", codes);
        }
    }
}
