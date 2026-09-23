package com.edusistem.core.auth.domain.inputports;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;

public interface PasswordRecoveryUseCase {

    /** Responde igual exista o no el correo para no permitir enumeración de usuarios. */
    void forgotPassword(AuthCommands.ForgotPassword command);

    void verifyCode(AuthCommands.VerifyResetCode command);

    void resetPassword(AuthCommands.ResetPassword command);
}
