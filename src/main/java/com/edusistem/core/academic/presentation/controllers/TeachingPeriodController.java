package com.edusistem.core.academic.presentation.controllers;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodUseCase;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.TeachingPeriodRequest;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.TeachingPeriodResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teaching-periods")
@Tag(name = "Teaching periods")
public class TeachingPeriodController {

    private final ManageTeachingPeriodUseCase teachingPeriods;

    public TeachingPeriodController(ManageTeachingPeriodUseCase teachingPeriods) {
        this.teachingPeriods = teachingPeriods;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vincula una asignación docente propia con un periodo académico")
    public TeachingPeriodResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody TeachingPeriodRequest request) {
        return TeachingPeriodResponse.from(teachingPeriods.create(new AcademicCommands.CreateTeachingPeriod(
                user.id(), request.teachingAssignmentId(), request.academicPeriodId())));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de los periodos de enseñanza del profesor autenticado")
    public PageResponse<TeachingPeriodResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @RequestParam(required = false) Long teachingAssignmentId,
                                                     @RequestParam(required = false) Long academicPeriodId,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        return PageResponse.from(teachingPeriods.list(user.id(), teachingAssignmentId, academicPeriodId,
                PageQuery.of(page, size)), TeachingPeriodResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un periodo de enseñanza propio")
    public TeachingPeriodResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return TeachingPeriodResponse.from(teachingPeriods.get(user.id(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un periodo de enseñanza sin evaluaciones ni configuración")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        teachingPeriods.delete(user.id(), id);
    }
}
