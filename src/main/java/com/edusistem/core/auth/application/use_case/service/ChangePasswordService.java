package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.application.use_case.dtos.AuthResult;
import com.edusistem.core.auth.domain.inputports.ChangePasswordUseCase;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.auth.domain.vo.PasswordPolicy;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordService implements ChangePasswordUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final SessionService sessions;
    private final RecordAuditUseCase audit;

    public ChangePasswordService(UserRepositoryPort users, PasswordHasherPort hasher, SessionService sessions,
                                 RecordAuditUseCase audit) {
        this.users = users;
        this.hasher = hasher;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Override
    @Transactional
    public AuthResult changePassword(AuthCommands.ChangePassword command) {
        User user = users.findById(command.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", command.userId()));
        if (!hasher.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new InvalidRequestException("INVALID_CURRENT_PASSWORD", "The current password is incorrect");
        }
        PasswordPolicy.validate(command.newPassword());
        user.changePasswordHash(hasher.hash(command.newPassword()));
        User saved = sessions.revokeAll(user); // invalida sesiones anteriores y guarda el nuevo hash
        audit.success(user.getId(), AuditAction.UPDATE, "User", user.getId(), "password changed; previous sessions revoked");
        return sessions.start(saved);
    }
}
