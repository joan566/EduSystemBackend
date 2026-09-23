package com.edusistem.core.auth.application.use_case.dtos;

public final class AuthCommands {

    private AuthCommands() {
    }

    public record Register(String firstName, String lastName, String email, String password) {
    }

    public record Login(String email, String password, String clientIp) {
    }

    public record ChangePassword(Long userId, String currentPassword, String newPassword) {
    }

    public record ForgotPassword(String email) {
    }

    public record VerifyResetCode(String email, String code) {
    }

    public record ResetPassword(String email, String code, String newPassword) {
    }
}
