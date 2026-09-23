package com.edusistem.core.auth.domain.inputports;

import com.edusistem.core.auth.application.use_case.dtos.AuthResult;

public interface RefreshSessionUseCase {

    /** Intercambia un refresh token válido por un nuevo par (access + refresh); el anterior queda revocado. */
    AuthResult refresh(String refreshToken);
}
