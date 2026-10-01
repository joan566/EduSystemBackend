package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.domain.inputports.RegisterUserUseCase;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.auth.domain.outputports.RequestRateLimitPort;
import com.edusistem.core.auth.domain.vo.RateLimit;
import com.edusistem.core.auth.domain.vo.PasswordPolicy;
import com.edusistem.core.authorization.domain.entity.Role;
import com.edusistem.core.authorization.domain.enums.RoleName;
import com.edusistem.core.authorization.domain.outputports.RoleRepositoryPort;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.TooManyRequestsException;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.util.EnumSet;
import java.util.Locale;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort users;
    private final RoleRepositoryPort roles;
    private final PasswordHasherPort hasher;
    private final EmailVerificationService emailVerification;
    private final RecordAuditUseCase audit;
    private final RequestRateLimitPort rateLimiter;
    private final RateLimit perIpLimit;

    public RegisterUserService(UserRepositoryPort users, RoleRepositoryPort roles, PasswordHasherPort hasher,
                               EmailVerificationService emailVerification, RecordAuditUseCase audit, RequestRateLimitPort rateLimiter,
                               RateLimit perIpLimit) {
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
        this.emailVerification = emailVerification;
        this.audit = audit;
        this.rateLimiter = rateLimiter;
        this.perIpLimit = perIpLimit;
    }

    @Override
    @UseCaseTransactional
    public User register(AuthCommands.Register command) {
        // Limita el alta masiva de cuentas y el sondeo de correos registrados (EMAIL_ALREADY_REGISTERED).
        long retryAfter = rateLimiter.tryConsume("register:ip:" + command.clientIp(), perIpLimit);
        if (retryAfter > 0) {
            throw new TooManyRequestsException("TOO_MANY_REGISTRATIONS",
                    "Too many sign-up attempts; try again later", retryAfter);
        }
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        PasswordPolicy.validate(command.password());
        if (users.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_REGISTERED", "The email is already registered");
        }
        Role teacher = roles.findByName(RoleName.TEACHER)
                .orElseThrow(() -> new BusinessRuleException("ROLE_NOT_CONFIGURED", "Role TEACHER is not configured"));
        User user = User.builder()
                .firstName(command.firstName().trim())
                .lastName(command.lastName().trim())
                .email(email)
                .passwordHash(hasher.hash(command.password()))
                .active(true)
                .emailVerified(false)
                .roles(EnumSet.of(teacher.getName()))
                .build();
        User saved = users.save(user);
        audit.success(saved.getId(), AuditAction.CREATE, "User", saved.getId(), "account registered");
        emailVerification.sendCode(saved);
        return saved;
    }
}
