package com.edusistem.core.auth.domain.inputports;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.user.domain.entity.User;

public interface RegisterUserUseCase {

    /** Crea la cuenta sin verificar y envía el código de verificación; no inicia sesión. */
    User register(AuthCommands.Register command);
}
