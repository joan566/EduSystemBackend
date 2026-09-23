package com.edusistem.core.activity.presentation.controllers;

import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.inputports.ManageActivityUseCase;
import com.edusistem.core.activity.presentation.dtos.ActivityDtos.ActivityResponse;
import com.edusistem.core.activity.presentation.dtos.ActivityDtos.CreateActivityRequest;
import com.edusistem.core.activity.presentation.dtos.ActivityDtos.RecordGradesRequest;
import com.edusistem.core.activity.presentation.dtos.ActivityDtos.SingleGradeRequest;
import com.edusistem.core.activity.presentation.dtos.ActivityDtos.StudentGradeResponse;
import com.edusistem.core.activity.presentation.dtos.ActivityDtos.UpdateActivityRequest;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import com.edusistem.core.shared.presentation.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activities")
@Tag(name = "Activities")
public class ActivityController {

    private final ManageActivityUseCase activities;
    private final GradeActivityUseCase gradeActivity;

    public ActivityController(ManageActivityUseCase activities, GradeActivityUseCase gradeActivity) {
        this.activities = activities;
        this.gradeActivity = gradeActivity;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una actividad calificable manualmente (categoría ACTIVITIES)")
    public ActivityResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody CreateActivityRequest request) {
        return ActivityResponse.from(activities.create(new ActivityCommands.Create(user.id(), request.teachingPeriodId(),
                request.name(), request.description(), request.evaluationDate(), request.maximumScore(),
                request.activityType())));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de actividades de un teaching period propio")
    public PageResponse<ActivityResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestParam Long teachingPeriodId,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return PageResponse.from(activities.search(user.id(), teachingPeriodId, PageQuery.of(page, size)),
                ActivityResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una actividad propia")
    public ActivityResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return ActivityResponse.from(activities.get(user.id(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una actividad")
    public ActivityResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                   @Valid @RequestBody UpdateActivityRequest request) {
        return ActivityResponse.from(activities.update(new ActivityCommands.Update(user.id(), id, request.name(),
                request.description(), request.evaluationDate(), request.maximumScore(), request.activityType())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una actividad sin notas")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        activities.delete(user.id(), id);
    }

    @GetMapping("/{id}/grades")
    @Operation(summary = "Notas de la actividad: un elemento por estudiante activo del grupo")
    public List<StudentGradeResponse> grades(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return gradeActivity.listGrades(user.id(), id).stream().map(StudentGradeResponse::from).toList();
    }

    @PutMapping("/{id}/grades")
    @Operation(summary = "Registra o corrige notas de varios estudiantes (0 ≤ nota ≤ puntaje máximo)")
    public List<StudentGradeResponse> recordGrades(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                                   @Valid @RequestBody RecordGradesRequest request) {
        List<ActivityCommands.GradeInput> inputs = request.grades().stream()
                .map(g -> new ActivityCommands.GradeInput(g.studentId(), g.grade(), g.comment())).toList();
        return gradeActivity.recordGrades(new ActivityCommands.RecordGrades(user.id(), id, inputs)).stream()
                .map(StudentGradeResponse::from).toList();
    }

    @PutMapping("/{id}/grades/{studentId}")
    @Operation(summary = "Registra o corrige la nota de un estudiante")
    public List<StudentGradeResponse> recordGrade(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                                  @PathVariable Long studentId,
                                                  @Valid @RequestBody SingleGradeRequest request) {
        return gradeActivity.recordGrades(new ActivityCommands.RecordGrades(user.id(), id,
                        List.of(new ActivityCommands.GradeInput(studentId, request.grade(), request.comment())))).stream()
                .map(StudentGradeResponse::from).toList();
    }
}
