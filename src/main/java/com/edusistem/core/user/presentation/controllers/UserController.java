package com.edusistem.core.user.presentation.controllers;

import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.user.application.use_case.dtos.UpdateProfileCommand;
import com.edusistem.core.user.domain.inputports.GetCurrentUserUseCase;
import com.edusistem.core.user.domain.inputports.UpdateProfileUseCase;
import com.edusistem.core.user.presentation.dtos.UpdateProfileRequest;
import com.edusistem.core.user.presentation.dtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUser;
    private final UpdateProfileUseCase updateProfile;

    public UserController(GetCurrentUserUseCase getCurrentUser, UpdateProfileUseCase updateProfile) {
        this.getCurrentUser = getCurrentUser;
        this.updateProfile = updateProfile;
    }

    @GetMapping("/me")
    @Operation(summary = "Consulta el perfil del usuario autenticado")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(getCurrentUser.getById(principal.id()));
    }

    @PutMapping("/me")
    @Operation(summary = "Actualiza nombre y apellido del usuario autenticado")
    public UserResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(updateProfile.update(
                new UpdateProfileCommand(principal.id(), request.firstName(), request.lastName())));
    }
}
