package com.edusistem.core.auth.presentation.controllers;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.domain.inputports.ChangePasswordUseCase;
import com.edusistem.core.auth.domain.inputports.LoginUseCase;
import com.edusistem.core.auth.domain.inputports.PasswordRecoveryUseCase;
import com.edusistem.core.auth.domain.inputports.RefreshSessionUseCase;
import com.edusistem.core.auth.domain.inputports.RegisterUserUseCase;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.ChangePasswordRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.ForgotPasswordRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.LoginRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.RefreshRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.RegisterRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.ResetPasswordRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.VerifyCodeRequest;
import com.edusistem.core.auth.presentation.dtos.AuthResponse;
import com.edusistem.core.auth.presentation.dtos.MessageResponse;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.user.domain.inputports.GetCurrentUserUseCase;
import com.edusistem.core.user.presentation.dtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final RegisterUserUseCase register;
    private final LoginUseCase login;
    private final ChangePasswordUseCase changePassword;
    private final PasswordRecoveryUseCase recovery;
    private final GetCurrentUserUseCase currentUser;
    private final RefreshSessionUseCase refresh;

    public AuthController(RegisterUserUseCase register, LoginUseCase login, ChangePasswordUseCase changePassword,
                          PasswordRecoveryUseCase recovery, GetCurrentUserUseCase currentUser,
                          RefreshSessionUseCase refresh) {
        this.register = register;
        this.login = login;
        this.changePassword = changePassword;
        this.recovery = recovery;
        this.currentUser = currentUser;
        this.refresh = refresh;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Registra una cuenta de profesor y devuelve la sesión")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return AuthResponse.from(register.register(request.toCommand()));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Inicia sesión y devuelve access token (JWT) y refresh token",
            description = "Tras varios intentos fallidos responde 429 con cabecera Retry-After.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return AuthResponse.from(login.login(request.toCommand(http.getRemoteAddr())));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renueva la sesión con un refresh token (rotativo: el anterior deja de servir)")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return AuthResponse.from(refresh.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cierra sesión: revoca todos los tokens del usuario (en todos los dispositivos)")
    public void logout(@AuthenticationPrincipal AuthenticatedUser user) {
        login.logout(user.id());
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve el usuario autenticado")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return UserResponse.from(currentUser.getById(user.id()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Cambia la contraseña; cierra las demás sesiones y devuelve una sesión nueva")
    public AuthResponse changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        return AuthResponse.from(changePassword.changePassword(
                new AuthCommands.ChangePassword(user.id(), request.currentPassword(), request.newPassword())));
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements
    @Operation(summary = "Solicita un código de recuperación por correo (respuesta idéntica exista o no el correo)")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        recovery.forgotPassword(new AuthCommands.ForgotPassword(request.email()));
        return new MessageResponse("If the email is registered, a verification code has been sent");
    }

    @PostMapping("/verify-code")
    @SecurityRequirements
    @Operation(summary = "Verifica un código de recuperación")
    public MessageResponse verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        recovery.verifyCode(new AuthCommands.VerifyResetCode(request.email(), request.code()));
        return new MessageResponse("The code is valid");
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Restablece la contraseña con un código válido")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        recovery.resetPassword(
                new AuthCommands.ResetPassword(request.email(), request.code(), request.newPassword()));
        return new MessageResponse("Password updated");
    }
}
