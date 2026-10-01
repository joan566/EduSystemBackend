package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.domain.entity.PasswordResetToken;
import com.edusistem.core.auth.domain.enums.OneTimeCodePurpose;
import com.edusistem.core.auth.domain.inputports.EmailVerificationUseCase;
import com.edusistem.core.auth.domain.outputports.MailSenderPort;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.auth.domain.outputports.PasswordResetTokenRepositoryPort;
import com.edusistem.core.auth.domain.outputports.RequestRateLimitPort;
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
 * Verificación del correo con código de 6 dígitos, con las mismas reglas que la recuperación de contraseña: solo se
 * guarda el hash, solo vale el último código, caduca, admite pocos intentos y se consume al usarlo. Las respuestas y el
 * trabajo criptográfico son los mismos exista o no la cuenta.
 */
public class EmailVerificationService implements EmailVerificationUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Límites por correo y por IP para reenviar códigos y para verificarlos. */
    public record Limits(RateLimit resendPerEmail, RateLimit resendPerIp, RateLimit verifyPerEmail,
                         RateLimit verifyPerIp) {
    }

    private final UserRepositoryPort users;
    private final PasswordResetTokenRepositoryPort tokens;
    private final PasswordHasherPort hasher;
    private final MailSenderPort mailSender;
    private final RecordAuditUseCase audit;
    private final RequestRateLimitPort rateLimiter;
    private final Limits limits;
    private final Clock clock;
    private final int ttlMinutes;
    private final int maxAttempts;
    private final String dummyHash;

    public EmailVerificationService(UserRepositoryPort users, PasswordResetTokenRepositoryPort tokens,
                                    PasswordHasherPort hasher, MailSenderPort mailSender, RecordAuditUseCase audit,
                                    RequestRateLimitPort rateLimiter, Limits limits, Clock clock, int ttlMinutes,
                                    int maxAttempts) {
        this.users = users;
        this.tokens = tokens;
        this.hasher = hasher;
        this.mailSender = mailSender;
        this.audit = audit;
        this.rateLimiter = rateLimiter;
        this.limits = limits;
        this.clock = clock;
        this.ttlMinutes = ttlMinutes;
        this.maxAttempts = maxAttempts;
        this.dummyHash = hasher.hash("000000");
    }

    /** Genera un código nuevo (el anterior deja de valer) y lo envía. Debe llamarse dentro de una transacción. */
    void sendCode(User user) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        tokens.save(PasswordResetToken.builder().userId(user.getId()).purpose(OneTimeCodePurpose.EMAIL_VERIFICATION)
                .codeHash(hasher.hash(code)).expiresAt(LocalDateTime.now(clock).plusMinutes(ttlMinutes)).attempts(0)
                .build());
        mailSender.sendEmailVerificationCode(user.getEmail(), code, ttlMinutes);
    }

    @Override
    @UseCaseTransactional
    public void resendVerification(AuthCommands.ResendVerification command) {
        String email = normalize(command.email());
        requireWithinLimit("resend-verification:email:" + email, limits.resendPerEmail());
        requireWithinLimit("resend-verification:ip:" + command.clientIp(), limits.resendPerIp());
        Optional<User> user = users.findByEmail(email).filter(User::isActive).filter(u -> !u.isEmailVerified());
        if (user.isEmpty()) {
            hasher.hash("000000"); // mismo coste que generar un código real
            return;
        }
        sendCode(user.get());
    }

    /** Los intentos fallidos se conservan (noRollbackFor) para poder limitar la fuerza bruta. */
    @Override
    @UseCaseTransactional(noRollbackFor = InvalidRequestException.class)
    public void verifyEmail(AuthCommands.VerifyEmail command) {
        String email = normalize(command.email());
        requireWithinLimit("verify-email:email:" + email, limits.verifyPerEmail());
        requireWithinLimit("verify-email:ip:" + command.clientIp(), limits.verifyPerIp());
        InvalidRequestException invalid = new InvalidRequestException("INVALID_VERIFICATION_CODE",
                "The code is invalid or has expired");
        Optional<User> user = users.findByEmail(email).filter(User::isActive).filter(u -> !u.isEmailVerified());
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<PasswordResetToken> token = user
                .flatMap(u -> tokens.findLatestByUserId(u.getId(), OneTimeCodePurpose.EMAIL_VERIFICATION))
                .filter(t -> t.isUsable(now, maxAttempts));
        if (token.isEmpty()) {
            hasher.matches(command.code(), dummyHash); // mismo coste que una verificación real
            throw invalid;
        }
        if (!hasher.matches(command.code(), token.get().getCodeHash())) {
            token.get().setAttempts(token.get().getAttempts() + 1);
            tokens.save(token.get());
            throw invalid;
        }
        user.get().markEmailVerified();
        users.save(user.get());
        token.get().setUsedAt(now);
        tokens.save(token.get());
        audit.success(user.get().getId(), AuditAction.UPDATE, "User", user.get().getId(), "email verified");
    }

    private void requireWithinLimit(String key, RateLimit limit) {
        long retryAfter = rateLimiter.tryConsume(key, limit);
        if (retryAfter > 0) {
            throw new TooManyRequestsException("TOO_MANY_EMAIL_VERIFICATION_REQUESTS",
                    "Too many email verification attempts; try again later", retryAfter);
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
