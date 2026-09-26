package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.application.use_case.dtos.AuthResult;
import com.edusistem.core.auth.domain.inputports.RegisterUserUseCase;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.auth.domain.vo.PasswordPolicy;
import com.edusistem.core.authorization.domain.entity.Role;
import com.edusistem.core.authorization.domain.enums.RoleName;
import com.edusistem.core.authorization.domain.outputports.RoleRepositoryPort;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.util.EnumSet;
import java.util.Locale;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepositoryPort users;
    private final RoleRepositoryPort roles;
    private final PasswordHasherPort hasher;
    private final SessionService sessions;
    private final RecordAuditUseCase audit;

    public RegisterUserService(UserRepositoryPort users, RoleRepositoryPort roles, PasswordHasherPort hasher,
                               SessionService sessions, RecordAuditUseCase audit) {
        this.users = users;
        this.roles = roles;
        this.hasher = hasher;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public AuthResult register(AuthCommands.Register command) {
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
                .roles(EnumSet.of(teacher.getName()))
                .build();
        User saved = users.save(user);
        audit.success(saved.getId(), AuditAction.CREATE, "User", saved.getId(), "account registered");
        return sessions.start(saved);
    }
}
