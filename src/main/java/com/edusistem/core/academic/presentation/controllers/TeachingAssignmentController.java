package com.edusistem.core.academic.presentation.controllers;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageTeachingAssignmentUseCase;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.ActiveRequest;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.TeachingAssignmentRequest;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.TeachingAssignmentResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teaching-assignments")
@Tag(name = "Teaching assignments")
public class TeachingAssignmentController {

    private final ManageTeachingAssignmentUseCase assignments;

    public TeachingAssignmentController(ManageTeachingAssignmentUseCase assignments) {
        this.assignments = assignments;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Asigna al profesor autenticado un grupo y una asignatura")
    public TeachingAssignmentResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @RequestBody TeachingAssignmentRequest request) {
        return TeachingAssignmentResponse.from(assignments.create(
                new AcademicCommands.CreateTeachingAssignment(user.id(), request.groupId(), request.subjectId())));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de las asignaciones del profesor autenticado")
    public PageResponse<TeachingAssignmentResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @RequestParam(required = false) Long groupId,
                                                         @RequestParam(required = false) Long subjectId,
                                                         @RequestParam(required = false) Boolean active,
                                                         @RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size) {
        return PageResponse.from(assignments.list(user.id(), groupId, subjectId, active, PageQuery.of(page, size)),
                TeachingAssignmentResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una asignación propia")
    public TeachingAssignmentResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return TeachingAssignmentResponse.from(assignments.get(user.id(), id));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activa o desactiva una asignación")
    public TeachingAssignmentResponse setActive(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                                @Valid @RequestBody ActiveRequest request) {
        return TeachingAssignmentResponse.from(assignments.setActive(user.id(), id, request.active()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una asignación sin periodos configurados")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        assignments.delete(user.id(), id);
    }
}
