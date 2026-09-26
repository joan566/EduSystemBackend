package com.edusistem.core.user.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.user.application.use_case.dtos.UpdateProfileCommand;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.inputports.GetCurrentUserUseCase;
import com.edusistem.core.user.domain.inputports.UpdateProfileUseCase;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;

public class UserProfileService implements GetCurrentUserUseCase, UpdateProfileUseCase {

    private final UserRepositoryPort users;
    private final RecordAuditUseCase audit;

    public UserProfileService(UserRepositoryPort users, RecordAuditUseCase audit) {
        this.users = users;
        this.audit = audit;
    }

    @Override
    public User getById(Long userId) {
        return users.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    /** Solo nombre y apellido: id, email, roles y estado no son editables desde el perfil. */
    @Override
    @UseCaseTransactional
    public User update(UpdateProfileCommand command) {
        User user = getById(command.userId());
        user.updateProfile(command.firstName(), command.lastName());
        User saved = users.save(user);
        audit.success(saved.getId(), AuditAction.UPDATE, "User", saved.getId(), "profile updated");
        return saved;
    }
}
