package com.edusistem.core.subject.presentation.controllers;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
import com.edusistem.core.subject.application.use_case.dtos.SubjectCommands;
import com.edusistem.core.subject.domain.inputports.ManageSubjectUseCase;
import com.edusistem.core.subject.presentation.dtos.SubjectDtos.Request;
import com.edusistem.core.subject.presentation.dtos.SubjectDtos.Response;
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
@RequestMapping("/api/v1/subjects")
@Tag(name = "Subjects")
public class SubjectController {

    private final ManageSubjectUseCase subjects;

    public SubjectController(ManageSubjectUseCase subjects) {
        this.subjects = subjects;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una asignatura")
    public Response create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody Request request) {
        return Response.from(subjects.create(new SubjectCommands.Create(user.id(), request.name(), request.description())));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de asignaturas (filtro opcional por nombre)")
    public PageResponse<Response> list(@RequestParam(required = false) String name,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size) {
        return PageResponse.from(subjects.search(name, PageQuery.of(page, size)), Response::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una asignatura")
    public Response get(@PathVariable Long id) {
        return Response.from(subjects.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una asignatura")
    public Response update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                           @Valid @RequestBody Request request) {
        return Response.from(subjects.update(
                new SubjectCommands.Update(user.id(), id, request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una asignatura sin asignaciones docentes")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        subjects.delete(user.id(), id);
    }
}
