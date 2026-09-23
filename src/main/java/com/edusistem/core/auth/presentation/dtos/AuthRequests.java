package com.edusistem.core.auth.presentation.dtos;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthRequests {

    private AuthRequests() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {

        public AuthCommands.Register toCommand() {
            return new AuthCommands.Register(firstName, lastName, email, password);
        }
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

        public AuthCommands.Login toCommand(String clientIp) {
            return new AuthCommands.Login(email, password, clientIp);
        }
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record VerifyCodeRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code") String code) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code") String code,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }
}
