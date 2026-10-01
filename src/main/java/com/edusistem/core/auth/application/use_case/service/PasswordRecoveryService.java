package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.domain.entity.PasswordResetToken;
import com.edusistem.core.auth.domain.enums.OneTimeCodePurpose;
import com.edusistem.core.auth.domain.inputports.PasswordRecoveryUseCase;
import com.edusistem.core.auth.domain.outputports.MailSenderPort;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.auth.domain.outputports.PasswordResetTokenRepositoryPort;
import com.edusistem.core.auth.domain.outputports.RequestRateLimitPort;
import com.edusistem.core.auth.domain.vo.PasswordPolicy;
import com.edusistem.core.auth.domain.vo.RateLimit;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.TooManyRequestsException;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Recuperación de contraseña con código de 6 dígitos. Para no revelar qué correos existen, las respuestas, los límites
 * de peticiones y el trabajo criptográfico son los mismos exista o no la cuenta (el correo se envía en segundo plano).
 */
public class PasswordRecoveryService implements PasswordRecoveryUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Límites por correo y por IP para solicitar códigos y para verificarlos. */
    public record Limits(RateLimit requestPerEmail, RateLimit requestPerIp, RateLimit verifyPerEmail,
                         RateLimit verifyPerIp) {
    }

    private final UserRepositoryPort users;
    private final PasswordResetTokenRepositoryPort tokens;
    private final PasswordHasherPort hasher;
    private final MailSenderPort mailSender;
    private final SessionService sessions;
    private final RecordAuditUseCase audit;
    private final RequestRateLimitPort rateLimiter;
    private final Limits limits;
    private final Clock clock;
    private final int ttlMinutes;
    private final int maxAttempts;
    private final String dummyHash;

    public PasswordRecoveryService(UserRepositoryPort users, PasswordResetTokenRepositoryPort tokens,
                                   PasswordHasherPort hasher, MailSenderPort mailSender, SessionService sessions,
                                   RecordAuditUseCase audit, RequestRateLimitPort rateLimiter, Limits limits,
                                   Clock clock, int ttlMinutes, int maxAttempts) {
        this.users = users;
        this.tokens = tokens;
        this.hasher = hasher;
        this.mailSender = mailSender;
        this.sessions = sessions;
        this.audit = audit;
        this.rateLimiter = rateLimiter;
        this.limits = limits;
        this.clock = clock;
        this.ttlMinutes = ttlMinutes;
        this.maxAttempts = maxAttempts;
        this.dummyHash = hasher.hash("000000");
    }

    @Override
    @UseCaseTransactional
    public void forgotPassword(AuthCommands.ForgotPassword command) {
        String email = normalize(command.email());
        requireWithinLimit("forgot:email:" + email, limits.requestPerEmail());
        requireWithinLimit("forgot:ip:" + command.clientIp(), limits.requestPerIp());
        Optional<User> user = users.findByEmail(email).filter(User::isActive);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String codeHash = hasher.hash(code); // también sin cuenta, para igualar el tiempo de respuesta
        if (user.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        tokens.save(PasswordResetToken.builder().userId(user.get().getId()).codeHash(codeHash)
                .expiresAt(now.plusMinutes(ttlMinutes)).attempts(0).build());
        mailSender.sendPasswordResetCode(user.get().getEmail(), code, ttlMinutes);
    }

    @Override
    @UseCaseTransactional(noRollbackFor = InvalidRequestException.class)
    public void verifyCode(AuthCommands.VerifyResetCode command) {
        requireValidCode(command.email(), command.code(), command.clientIp());
    }

    /** Los intentos fallidos se conservan (noRollbackFor) para poder limitar la fuerza bruta. */
    @Override
    @UseCaseTransactional(noRollbackFor = InvalidRequestException.class)
    public void resetPassword(AuthCommands.ResetPassword command) {
        PasswordPolicy.validate(command.newPassword());
        ValidCode valid = requireValidCode(command.email(), command.code(), command.clientIp());
        valid.user().changePasswordHash(hasher.hash(command.newPassword()));
        sessions.revokeAll(valid.user()); // guarda el nuevo hash y cierra todas las sesiones
        valid.token().setUsedAt(LocalDateTime.now(clock));
        tokens.save(valid.token());
        audit.success(valid.user().getId(), AuditAction.UPDATE, "User", valid.user().getId(), "password reset");
    }

    /**
     * Además de los 5 intentos por código, limita las verificaciones por correo y por IP: sin eso, pedir códigos nuevos
     * sin parar permitiría adivinar uno por fuerza bruta.
     */
    private ValidCode requireValidCode(String rawEmail, String code, String clientIp) {
        String email = normalize(rawEmail);
        requireWithinLimit("verify:email:" + email, limits.verifyPerEmail());
        requireWithinLimit("verify:ip:" + clientIp, limits.verifyPerIp());
        InvalidRequestException invalid = new InvalidRequestException("INVALID_RESET_CODE",
                "The code is invalid or has expired");
        Optional<User> user = users.findByEmail(email).filter(User::isActive);
        Optional<PasswordResetToken> token = user.flatMap(u -> tokens.findLatestByUserId(u.getId(), OneTimeCodePurpose.PASSWORD_RESET))
                .filter(t -> t.isUsable(LocalDateTime.now(clock), maxAttempts));
        if (token.isEmpty()) {
            hasher.matches(code, dummyHash); // mismo coste que una verificación real
            throw invalid;
        }
        if (!hasher.matches(code, token.get().getCodeHash())) {
            token.get().setAttempts(token.get().getAttempts() + 1);
            tokens.save(token.get());
            throw invalid;
        }
        return new ValidCode(user.get(), token.get());
    }

    private void requireWithinLimit(String key, RateLimit limit) {
        long retryAfter = rateLimiter.tryConsume(key, limit);
        if (retryAfter > 0) {
            throw new TooManyRequestsException("TOO_MANY_PASSWORD_RESET_REQUESTS",
                    "Too many password recovery attempts; try again later", retryAfter);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record ValidCode(User user, PasswordResetToken token) {
    }
}
