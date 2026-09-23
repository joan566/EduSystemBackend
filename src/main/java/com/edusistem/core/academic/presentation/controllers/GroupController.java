package com.edusistem.core.academic.presentation.controllers;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageGroupUseCase;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.CreateGroupRequest;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.GroupResponse;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.UpdateGroupRequest;
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
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups")
public class GroupController {

    private final ManageGroupUseCase groups;

    public GroupController(ManageGroupUseCase groups) {
        this.groups = groups;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un grupo dentro de un grado y año académico")
    public GroupResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody CreateGroupRequest request) {
        return GroupResponse.from(groups.create(new AcademicCommands.CreateGroup(user.id(), request.gradeId(),
                request.name(), request.academicYear())));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de grupos (filtros por grado y año)")
    public PageResponse<GroupResponse> list(@RequestParam(required = false) Long gradeId,
                                            @RequestParam(required = false) Integer academicYear,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return PageResponse.from(groups.search(gradeId, academicYear, PageQuery.of(page, size)), GroupResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta un grupo")
    public GroupResponse get(@PathVariable Long id) {
        return GroupResponse.from(groups.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza nombre y año de un grupo")
    public GroupResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                @Valid @RequestBody UpdateGroupRequest request) {
        return GroupResponse.from(groups.update(
                new AcademicCommands.UpdateGroup(user.id(), id, request.name(), request.academicYear())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un grupo sin estudiantes ni asignaciones docentes")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        groups.delete(user.id(), id);
    }
}
