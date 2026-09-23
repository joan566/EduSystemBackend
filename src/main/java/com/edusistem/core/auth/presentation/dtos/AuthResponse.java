package com.edusistem.core.auth.presentation.dtos;

import com.edusistem.core.auth.application.use_case.dtos.AuthResult;
import com.edusistem.core.user.presentation.dtos.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
                           UserResponse user) {

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.token().accessToken(), result.token().tokenType(),
                result.token().expiresInSeconds(), result.refreshToken(), UserResponse.from(result.user()));
    }
}
