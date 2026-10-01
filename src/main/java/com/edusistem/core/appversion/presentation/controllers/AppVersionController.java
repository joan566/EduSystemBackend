package com.edusistem.core.appversion.presentation.controllers;

import com.edusistem.core.appversion.domain.inputports.GetAppVersionPolicyUseCase;
import com.edusistem.core.appversion.presentation.dtos.AppVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/app")
@Tag(name = "App")
public class AppVersionController {

    private final GetAppVersionPolicyUseCase versions;

    public AppVersionController(GetAppVersionPolicyUseCase versions) {
        this.versions = versions;
    }

    @GetMapping("/version")
    @SecurityRequirements
    @Operation(summary = "Última versión de la app móvil y mínima permitida (público, no requiere autenticación)")
    public AppVersionResponse version() {
        return AppVersionResponse.from(versions.current());
    }
}
