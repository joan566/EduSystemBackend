package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.dtos.AuthResult;
import com.edusistem.core.auth.domain.entity.RefreshToken;
import com.edusistem.core.auth.domain.inputports.RefreshSessionUseCase;
import com.edusistem.core.auth.domain.outputports.RefreshTokenRepositoryPort;
import com.edusistem.core.auth.domain.vo.RefreshTokenCodec;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.UnauthorizedException;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.time.Clock;
import java.time.LocalDateTime;

public class RefreshSessionService implements RefreshSessionUseCase {

    private final RefreshTokenRepositoryPort refreshTokens;
    private final UserRepositoryPort users;
    private final SessionService sessions;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    public RefreshSessionService(RefreshTokenRepositoryPort refreshTokens, UserRepositoryPort users,
                                 SessionService sessions, RecordAuditUseCase audit, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.sessions = sessions;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Rotación con detección de reuso: presentar un refresh token ya usado indica robo, así que se cierran todas las
     * sesiones del usuario. noRollbackFor conserva esa revocación aunque se responda 401.
     */
    @Override
    @UseCaseTransactional(noRollbackFor = UnauthorizedException.class)
    public AuthResult refresh(String rawToken) {
        UnauthorizedException invalid = new UnauthorizedException("INVALID_REFRESH_TOKEN", "The refresh token is invalid or has expired");
        RefreshToken token = refreshTokens.findByTokenHash(RefreshTokenCodec.hash(rawToken)).orElseThrow(() -> invalid);
        LocalDateTime now = LocalDateTime.now(clock);
        User user = users.findById(token.getUserId()).filter(User::isActive).orElseThrow(() -> invalid);
        if (token.isRevoked()) {
            sessions.revokeAll(user);
            audit.success(user.getId(), AuditAction.LOGOUT, "User", user.getId(), "refresh token reuse detected: all sessions revoked");
            throw invalid;
        }
        if (token.isExpired(now)) {
            throw invalid;
        }
        token.setRevokedAt(now);
        refreshTokens.save(token);
        return sessions.start(user);
    }
}
