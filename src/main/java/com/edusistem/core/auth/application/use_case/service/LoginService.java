package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.application.use_case.dtos.AuthResult;
import com.edusistem.core.auth.domain.inputports.LoginUseCase;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.auth.domain.outputports.LoginAttemptPort;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.TooManyRequestsException;
import com.edusistem.core.shared.domain.exceptions.UnauthorizedException;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.util.Locale;
import java.util.Optional;

public class LoginService implements LoginUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final SessionService sessions;
    private final LoginAttemptPort attempts;
    private final RecordAuditUseCase audit;

    public LoginService(UserRepositoryPort users, PasswordHasherPort hasher, SessionService sessions,
                        LoginAttemptPort attempts, RecordAuditUseCase audit) {
        this.users = users;
        this.hasher = hasher;
        this.sessions = sessions;
        this.attempts = attempts;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public AuthResult login(AuthCommands.Login command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        long retryAfter = attempts.retryAfterSeconds(email, command.clientIp());
        if (retryAfter > 0) {
            audit.failure(null, AuditAction.LOGIN, "User", null, "blocked by rate limit for " + email);
            throw new TooManyRequestsException("TOO_MANY_LOGIN_ATTEMPTS",
                    "Too many failed login attempts; try again later", retryAfter);
        }
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty() || !found.get().isActive()
                || !hasher.matches(command.password(), found.get().getPasswordHash())) {
            attempts.recordFailure(email, command.clientIp());
            audit.failure(found.map(User::getId).orElse(null), AuditAction.LOGIN, "User",
                    found.map(User::getId).orElse(null), "invalid credentials for " + email);
            // Mismo mensaje para email inexistente, cuenta inactiva o contraseña errónea.
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password");
        }
        User user = found.get();
        attempts.recordSuccess(email);
        audit.success(user.getId(), AuditAction.LOGIN, "User", user.getId(), null);
        return sessions.start(user);
    }

    /** Revoca todos los JWT y refresh tokens del usuario (cierra la sesión en todos los dispositivos). */
    @Override
    @UseCaseTransactional
    public void logout(Long userId) {
        users.findById(userId).ifPresent(sessions::revokeAll);
        audit.success(userId, AuditAction.LOGOUT, "User", userId, null);
    }
}
