package com.edusistem.core.auth.domain.inputports;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;

public interface EmailVerificationUseCase {

    void verifyEmail(AuthCommands.VerifyEmail command);

    /** Responde igual exista o no el correo (o ya esté verificado) para no permitir enumeración de usuarios. */
    void resendVerification(AuthCommands.ResendVerification command);
}
