package com.edusistem.core.auth.presentation.controllers;

import com.edusistem.core.auth.application.use_case.dtos.AuthCommands;
import com.edusistem.core.auth.domain.inputports.ChangePasswordUseCase;
import com.edusistem.core.auth.domain.inputports.DeleteAccountUseCase;
import com.edusistem.core.auth.domain.inputports.EmailVerificationUseCase;
import com.edusistem.core.auth.domain.inputports.LoginUseCase;
import com.edusistem.core.auth.domain.inputports.PasswordRecoveryUseCase;
import com.edusistem.core.auth.domain.inputports.RefreshSessionUseCase;
import com.edusistem.core.auth.domain.inputports.RegisterUserUseCase;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.ChangePasswordRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.DeleteAccountRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.ForgotPasswordRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.LoginRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.RefreshRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.RegisterRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.ResendVerificationRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.ResetPasswordRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.VerifyCodeRequest;
import com.edusistem.core.auth.presentation.dtos.AuthRequests.VerifyEmailRequest;
import com.edusistem.core.auth.presentation.dtos.AuthResponse;
import com.edusistem.core.auth.presentation.dtos.MessageResponse;
import com.edusistem.core.auth.presentation.dtos.RegisterResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final DeleteAccountUseCase deleteAccount;
    private final EmailVerificationUseCase emailVerification;

    public AuthController(RegisterUserUseCase register, LoginUseCase login, ChangePasswordUseCase changePassword,
                          PasswordRecoveryUseCase recovery, GetCurrentUserUseCase currentUser,
                          RefreshSessionUseCase refresh, DeleteAccountUseCase deleteAccount,
                          EmailVerificationUseCase emailVerification) {
        this.register = register;
        this.login = login;
        this.changePassword = changePassword;
        this.recovery = recovery;
        this.currentUser = currentUser;
        this.refresh = refresh;
        this.deleteAccount = deleteAccount;
        this.emailVerification = emailVerification;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Registra una cuenta de profesor sin verificar y envía un código al correo",
            description = "No inicia sesión: hay que verificar el correo (POST /verify-email) y luego hacer login. "
                    + "Limitado por IP: al superarlo responde 429 con cabecera Retry-After.")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return RegisterResponse.from(register.register(request.toCommand(http.getRemoteAddr())));
    }

    @PostMapping("/verify-email")
    @SecurityRequirements
    @Operation(summary = "Verifica el correo de una cuenta nueva con el código recibido",
            description = "Limitado por correo y por IP: al superarlo responde 429 con cabecera Retry-After.")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request, HttpServletRequest http) {
        emailVerification.verifyEmail(new AuthCommands.VerifyEmail(request.email(), request.code(),
                http.getRemoteAddr()));
        return new MessageResponse("Email verified");
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements
    @Operation(summary = "Envía un nuevo código de verificación (el anterior deja de valer; respuesta idéntica "
            + "exista o no el correo)",
            description = "Limitado por correo y por IP: al superarlo responde 429 con cabecera Retry-After.")
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request,
                                              HttpServletRequest http) {
        emailVerification.resendVerification(new AuthCommands.ResendVerification(request.email(),
                http.getRemoteAddr()));
        return new MessageResponse("If the email is registered and pending verification, a new code has been sent");
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Inicia sesión y devuelve access token (JWT) y refresh token",
            description = "Con credenciales correctas pero correo sin verificar responde 403 EMAIL_NOT_VERIFIED. "
                    + "Tras varios intentos fallidos responde 429 con cabecera Retry-After.")
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

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina la cuenta y TODOS sus datos (estudiantes, notas, archivos...); pide la contraseña",
            description = "No se puede deshacer. Las sesiones dejan de valer de inmediato.")
    public void deleteAccount(@AuthenticationPrincipal AuthenticatedUser user,
                              @Valid @RequestBody DeleteAccountRequest request) {
        deleteAccount.deleteAccount(user.id(), request.password());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements
    @Operation(summary = "Solicita un código de recuperación por correo (respuesta idéntica exista o no el correo)",
            description = "Limitado por correo y por IP: al superarlo responde 429 con cabecera Retry-After.")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        recovery.forgotPassword(new AuthCommands.ForgotPassword(request.email(), http.getRemoteAddr()));
        return new MessageResponse("If the email is registered, a verification code has been sent");
    }

    @PostMapping("/verify-code")
    @SecurityRequirements
    @Operation(summary = "Verifica un código de recuperación")
    public MessageResponse verifyCode(@Valid @RequestBody VerifyCodeRequest request, HttpServletRequest http) {
        recovery.verifyCode(new AuthCommands.VerifyResetCode(request.email(), request.code(), http.getRemoteAddr()));
        return new MessageResponse("The code is valid");
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Restablece la contraseña con un código válido")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        recovery.resetPassword(new AuthCommands.ResetPassword(request.email(), request.code(), request.newPassword(),
                http.getRemoteAddr()));
        return new MessageResponse("Password updated");
    }
}
