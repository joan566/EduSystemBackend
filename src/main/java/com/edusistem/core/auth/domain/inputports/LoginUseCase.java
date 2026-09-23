package com.edusistem.core.auth.domain.inputports;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.application.use_case.dtos.AuthResult;

public interface LoginUseCase {

    AuthResult login(AuthCommands.Login command);

    /** Los JWT son stateless: el logout solo deja constancia en la auditoría. */
    void logout(Long userId);
}
