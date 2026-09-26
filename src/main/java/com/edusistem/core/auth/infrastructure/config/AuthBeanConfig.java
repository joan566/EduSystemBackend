package com.edusistem.core.auth.infrastructure.config;

import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.service.ChangePasswordService;
import com.edusistem.core.auth.application.use_case.service.LoginService;
import com.edusistem.core.auth.application.use_case.service.PasswordRecoveryService;
import com.edusistem.core.auth.application.use_case.service.RefreshSessionService;
import com.edusistem.core.auth.application.use_case.service.RegisterUserService;
import com.edusistem.core.auth.application.use_case.service.SessionService;
import com.edusistem.core.auth.domain.outputports.LoginAttemptPort;
import com.edusistem.core.auth.domain.outputports.MailSenderPort;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.auth.domain.outputports.PasswordResetTokenRepositoryPort;
import com.edusistem.core.auth.domain.outputports.RefreshTokenRepositoryPort;
import com.edusistem.core.auth.domain.outputports.TokenIssuerPort;
import com.edusistem.core.auth.infrastructure.security.JwtProperties;
import com.edusistem.core.authorization.domain.outputports.RoleRepositoryPort;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo auth. */
@Configuration
public class AuthBeanConfig {

    @Bean
    SessionService sessionService(TokenIssuerPort tokenIssuer, RefreshTokenRepositoryPort refreshTokens,
                                  UserRepositoryPort users, Clock clock, JwtProperties jwtProperties) {
        return new SessionService(tokenIssuer, refreshTokens, users, clock, jwtProperties.refreshExpirationDays());
    }

    @Bean
    ChangePasswordService changePasswordService(UserRepositoryPort users, PasswordHasherPort hasher,
                                                SessionService sessions, RecordAuditUseCase audit) {
        return new ChangePasswordService(users, hasher, sessions, audit);
    }

    @Bean
    LoginService loginService(UserRepositoryPort users, PasswordHasherPort hasher, SessionService sessions,
                              LoginAttemptPort attempts, RecordAuditUseCase audit) {
        return new LoginService(users, hasher, sessions, attempts, audit);
    }

    @Bean
    PasswordRecoveryService passwordRecoveryService(UserRepositoryPort users,
                                                    PasswordResetTokenRepositoryPort tokens,
                                                    PasswordHasherPort hasher, MailSenderPort mailSender,
                                                    SessionService sessions, RecordAuditUseCase audit, Clock clock,
                                                    @Value("${edusistem.password-reset.code-ttl-minutes}") int ttlMinutes,
                                                    @Value("${edusistem.password-reset.max-attempts}") int maxAttempts) {
        return new PasswordRecoveryService(users, tokens, hasher, mailSender, sessions, audit, clock, ttlMinutes,
                                           maxAttempts);
    }

    @Bean
    RefreshSessionService refreshSessionService(RefreshTokenRepositoryPort refreshTokens, UserRepositoryPort users,
                                                SessionService sessions, RecordAuditUseCase audit, Clock clock) {
        return new RefreshSessionService(refreshTokens, users, sessions, audit, clock);
    }

    @Bean
    RegisterUserService registerUserService(UserRepositoryPort users, RoleRepositoryPort roles,
                                            PasswordHasherPort hasher, SessionService sessions,
                                            RecordAuditUseCase audit) {
        return new RegisterUserService(users, roles, hasher, sessions, audit);
    }
}
