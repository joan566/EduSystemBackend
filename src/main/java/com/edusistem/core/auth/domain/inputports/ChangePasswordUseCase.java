package com.edusistem.core.auth.domain.inputports;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.application.use_case.dtos.AuthResult;

public interface ChangePasswordUseCase {

    /** Cierra todas las sesiones anteriores y devuelve una sesión nueva para el dispositivo actual. */
    AuthResult changePassword(AuthCommands.ChangePassword command);
}
