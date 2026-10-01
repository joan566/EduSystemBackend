package com.edusistem.core.auth.presentation.dtos;

import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.presentation.dtos.UserResponse;

/** Cuenta creada pero sin sesión: hay que verificar el correo (POST /auth/verify-email) y luego iniciar sesión. */
public record RegisterResponse(String message, UserResponse user) {

    public static RegisterResponse from(User user) {
        return new RegisterResponse("A verification code has been sent to the email address", UserResponse.from(user));
    }
}
