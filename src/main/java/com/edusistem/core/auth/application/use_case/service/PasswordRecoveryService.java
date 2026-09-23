package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.domain.entity.PasswordResetToken;
import com.edusistem.core.auth.domain.inputports.PasswordRecoveryUseCase;
import com.edusistem.core.auth.domain.outputports.MailSenderPort;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.auth.domain.outputports.PasswordResetTokenRepositoryPort;
import com.edusistem.core.auth.domain.vo.PasswordPolicy;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordRecoveryService implements PasswordRecoveryUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepositoryPort users;
    private final PasswordResetTokenRepositoryPort tokens;
    private final PasswordHasherPort hasher;
    private final MailSenderPort mailSender;
    private final SessionService sessions;
    private final RecordAuditUseCase audit;
    private final Clock clock;
    private final int ttlMinutes;
    private final int maxAttempts;

    public PasswordRecoveryService(UserRepositoryPort users, PasswordResetTokenRepositoryPort tokens,
                                   PasswordHasherPort hasher, MailSenderPort mailSender, SessionService sessions,
                                   RecordAuditUseCase audit, Clock clock,
                                   @Value("${edusistem.password-reset.code-ttl-minutes}") int ttlMinutes,
                                   @Value("${edusistem.password-reset.max-attempts}") int maxAttempts) {
        this.users = users;
        this.tokens = tokens;
        this.hasher = hasher;
        this.mailSender = mailSender;
        this.sessions = sessions;
        this.audit = audit;
        this.clock = clock;
        this.ttlMinutes = ttlMinutes;
        this.maxAttempts = maxAttempts;
    }

    @Override
    @Transactional
    public void forgotPassword(AuthCommands.ForgotPassword command) {
        Optional<User> user = users.findByEmail(normalize(command.email())).filter(User::isActive);
        if (user.isEmpty()) {
            return;
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now(clock);
        tokens.save(PasswordResetToken.builder().userId(user.get().getId()).codeHash(hasher.hash(code))
                .expiresAt(now.plusMinutes(ttlMinutes)).attempts(0).build());
        mailSender.sendPasswordResetCode(user.get().getEmail(), code, ttlMinutes);
    }

    @Override
    @Transactional(noRollbackFor = InvalidRequestException.class)
    public void verifyCode(AuthCommands.VerifyResetCode command) {
        requireValidCode(command.email(), command.code());
    }

    /** Los intentos fallidos se conservan (noRollbackFor) para poder limitar la fuerza bruta. */
    @Override
    @Transactional(noRollbackFor = InvalidRequestException.class)
    public void resetPassword(AuthCommands.ResetPassword command) {
        PasswordPolicy.validate(command.newPassword());
        ValidCode valid = requireValidCode(command.email(), command.code());
        valid.user().changePasswordHash(hasher.hash(command.newPassword()));
        sessions.revokeAll(valid.user()); // guarda el nuevo hash y cierra todas las sesiones
        valid.token().setUsedAt(LocalDateTime.now(clock));
        tokens.save(valid.token());
        audit.success(valid.user().getId(), AuditAction.UPDATE, "User", valid.user().getId(), "password reset");
    }

    private ValidCode requireValidCode(String email, String code) {
        InvalidRequestException invalid = new InvalidRequestException("INVALID_RESET_CODE",
                "The code is invalid or has expired");
        User user = users.findByEmail(normalize(email)).filter(User::isActive).orElseThrow(() -> invalid);
        PasswordResetToken token = tokens.findLatestByUserId(user.getId()).orElseThrow(() -> invalid);
        if (!token.isUsable(LocalDateTime.now(clock), maxAttempts)) {
            throw invalid;
        }
        if (!hasher.matches(code, token.getCodeHash())) {
            token.setAttempts(token.getAttempts() + 1);
            tokens.save(token);
            throw invalid;
        }
        return new ValidCode(user, token);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record ValidCode(User user, PasswordResetToken token) {
    }
}
