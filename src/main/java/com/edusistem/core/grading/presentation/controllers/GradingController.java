package com.edusistem.core.grading.presentation.controllers;

import com.edusistem.core.grading.application.use_case.dtos.GradingCommands;
import com.edusistem.core.grading.domain.inputports.CalculatePeriodGradeUseCase;
import com.edusistem.core.grading.domain.inputports.ConfigureGradingUseCase;
import com.edusistem.core.grading.domain.inputports.ManageGradingScaleUseCase;
import com.edusistem.core.grading.presentation.dtos.GradingDtos.ConfigurationRequest;
import com.edusistem.core.grading.presentation.dtos.GradingDtos.ConfigurationResponse;
import com.edusistem.core.grading.presentation.dtos.GradingDtos.PeriodGradeResponse;
import com.edusistem.core.grading.presentation.dtos.GradingDtos.ScaleRequest;
import com.edusistem.core.grading.presentation.dtos.GradingDtos.ScaleResponse;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Grading")
public class GradingController {

    private final ManageGradingScaleUseCase scales;
    private final ConfigureGradingUseCase configuration;
    private final CalculatePeriodGradeUseCase periodGrades;

    public GradingController(ManageGradingScaleUseCase scales, ConfigureGradingUseCase configuration,
                             CalculatePeriodGradeUseCase periodGrades) {
        this.scales = scales;
        this.configuration = configuration;
        this.periodGrades = periodGrades;
    }

    @PostMapping("/grading-scales")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una escala de calificación (mínimo < máximo)")
    public ScaleResponse createScale(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody ScaleRequest request) {
        return ScaleResponse.from(scales.create(new GradingCommands.CreateScale(user.id(), request.name(),
                request.minimumValue(), request.maximumValue())));
    }

    @GetMapping("/grading-scales")
    @Operation(summary = "Lista las escalas de calificación")
    public List<ScaleResponse> listScales() {
        return scales.list().stream().map(ScaleResponse::from).toList();
    }

    @GetMapping("/grading-scales/{id}")
    @Operation(summary = "Consulta una escala de calificación")
    public ScaleResponse getScale(@PathVariable Long id) {
        return ScaleResponse.from(scales.get(id));
    }

    @PutMapping("/teaching-periods/{teachingPeriodId}/grading-configuration")
    @Operation(summary = "Guarda escala y porcentajes de un teaching period (admite parciales; suma ≤ 100)")
    public ConfigurationResponse saveConfiguration(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long teachingPeriodId,
                                                   @Valid @RequestBody ConfigurationRequest request) {
        return ConfigurationResponse.from(configuration.save(new GradingCommands.SaveConfiguration(user.id(),
                teachingPeriodId, request.gradingScaleId(), request.toWeights())));
    }

    @GetMapping("/teaching-periods/{teachingPeriodId}/grading-configuration")
    @Operation(summary = "Consulta la configuración de calificación (incluye si está completa)")
    public ConfigurationResponse getConfiguration(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable Long teachingPeriodId) {
        return ConfigurationResponse.from(configuration.get(user.id(), teachingPeriodId));
    }

    @GetMapping("/teaching-periods/{teachingPeriodId}/period-grades")
    @Operation(summary = "Calcula la nota del periodo por estudiante (requiere pesos que sumen 100%)")
    public PeriodGradeResponse periodGrades(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable Long teachingPeriodId) {
        return PeriodGradeResponse.from(periodGrades.calculate(user.id(), teachingPeriodId));
    }
}
