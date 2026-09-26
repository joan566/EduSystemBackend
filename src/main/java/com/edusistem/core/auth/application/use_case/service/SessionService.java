package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.auth.application.use_case.dtos.AuthResult;
import com.edusistem.core.auth.domain.entity.RefreshToken;
import com.edusistem.core.auth.domain.outputports.RefreshTokenRepositoryPort;
import com.edusistem.core.auth.domain.outputports.TokenIssuerPort;
import com.edusistem.core.auth.domain.vo.RefreshTokenCodec;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.time.Clock;
import java.time.LocalDateTime;

/** Emisión y revocación de sesiones (access JWT + refresh token). Debe llamarse dentro de una transacción. */
public class SessionService {

    private final TokenIssuerPort tokenIssuer;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final UserRepositoryPort users;
    private final Clock clock;
    private final long refreshDays;

    public SessionService(TokenIssuerPort tokenIssuer, RefreshTokenRepositoryPort refreshTokens, UserRepositoryPort users,
                   Clock clock, long refreshDays) {
        this.tokenIssuer = tokenIssuer;
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.clock = clock;
        this.refreshDays = refreshDays;
    }

    AuthResult start(User user) {
        String raw = RefreshTokenCodec.generate();
        LocalDateTime now = LocalDateTime.now(clock);
        refreshTokens.save(RefreshToken.builder().userId(user.getId()).tokenHash(RefreshTokenCodec.hash(raw))
                .expiresAt(now.plusDays(refreshDays)).build());
        return new AuthResult(tokenIssuer.issue(user), raw, user);
    }

    /** Invalida todos los JWT (sube token_version) y todos los refresh tokens del usuario; guarda al usuario. */
    User revokeAll(User user) {
        user.revokeTokens();
        User saved = users.save(user);
        refreshTokens.revokeAllByUserId(saved.getId(), LocalDateTime.now(clock));
        return saved;
    }
}
