package com.edusistem.core.auth.domain.outputports;

import com.edusistem.core.auth.domain.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllByUserId(Long userId, LocalDateTime now);
}
