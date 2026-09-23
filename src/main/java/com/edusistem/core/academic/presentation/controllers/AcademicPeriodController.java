package com.edusistem.core.academic.presentation.controllers;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageAcademicPeriodUseCase;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.AcademicPeriodRequest;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.AcademicPeriodResponse;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/academic-periods")
@Tag(name = "Academic periods")
public class AcademicPeriodController {

    private final ManageAcademicPeriodUseCase periods;

    public AcademicPeriodController(ManageAcademicPeriodUseCase periods) {
        this.periods = periods;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un periodo académico")
    public AcademicPeriodResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody AcademicPeriodRequest request) {
        return AcademicPeriodResponse.from(periods.create(new AcademicCommands.SavePeriod(user.id(), null,
                request.name(), request.startDate(), request.endDate())));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de periodos académicos")
    public PageResponse<AcademicPeriodResponse> list(@RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        return PageResponse.from(periods.list(PageQuery.of(page, size)), AcademicPeriodResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un periodo académico")
    public AcademicPeriodResponse get(@PathVariable Long id) {
        return AcademicPeriodResponse.from(periods.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un periodo académico")
    public AcademicPeriodResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                         @Valid @RequestBody AcademicPeriodRequest request) {
        return AcademicPeriodResponse.from(periods.update(new AcademicCommands.SavePeriod(user.id(), id,
                request.name(), request.startDate(), request.endDate())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un periodo académico sin uso")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        periods.delete(user.id(), id);
    }
}
