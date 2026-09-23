package com.edusistem.core.academic.presentation.controllers;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageGradeUseCase;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.GradeRequest;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.GradeResponse;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/grades")
@Tag(name = "Grades")
public class GradeController {

    private final ManageGradeUseCase grades;

    public GradeController(ManageGradeUseCase grades) {
        this.grades = grades;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un grado")
    public GradeResponse create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody GradeRequest request) {
        return GradeResponse.from(grades.create(
                new AcademicCommands.CreateGrade(user.id(), request.name(), request.description())));
    }

    @GetMapping
    @Operation(summary = "Lista todos los grados (catálogo corto, sin paginar)")
    public List<GradeResponse> list() {
        return grades.list().stream().map(GradeResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un grado")
    public GradeResponse get(@PathVariable Long id) {
        return GradeResponse.from(grades.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un grado")
    public GradeResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                @Valid @RequestBody GradeRequest request) {
        return GradeResponse.from(grades.update(
                new AcademicCommands.UpdateGrade(user.id(), id, request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un grado sin grupos")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        grades.delete(user.id(), id);
    }
}
