package com.edusistem.core.auth.application.use_case.service;

import com.edusistem.core.auth.domain.inputports.DeleteAccountUseCase;
import com.edusistem.core.auth.domain.outputports.AccountErasurePort;
import com.edusistem.core.auth.domain.outputports.PasswordHasherPort;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.domain.outputports.UserRepositoryPort;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** No deja auditoría: la auditoría del usuario también se borra, y el JWT deja de valer al no existir el usuario. */
public class DeleteAccountService implements DeleteAccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteAccountService.class);

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final AccountErasurePort erasure;
    private final FileStoragePort storage;

    public DeleteAccountService(UserRepositoryPort users, PasswordHasherPort hasher, AccountErasurePort erasure,
                                FileStoragePort storage) {
        this.users = users;
        this.hasher = hasher;
        this.erasure = erasure;
        this.storage = storage;
    }

    @Override
    @UseCaseTransactional
    public void deleteAccount(Long userId, String password) {
        User user = users.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        if (!hasher.matches(password, user.getPasswordHash())) {
            throw new InvalidRequestException("INVALID_CURRENT_PASSWORD", "The current password is incorrect");
        }
        int failed = 0;
        for (String path : erasure.erase(userId, user.getEmail())) {
            try {
                storage.delete(path);
            } catch (IOException | RuntimeException e) {
                failed++;
            }
        }
        if (failed > 0) {
            log.warn("Account {} deleted; {} file(s) could not be removed from storage", userId, failed);
        } else {
            log.info("Account {} deleted with all its data", userId);
        }
    }
}
