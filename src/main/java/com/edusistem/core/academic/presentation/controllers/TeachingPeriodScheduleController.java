package com.edusistem.core.academic.presentation.controllers;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodScheduleUseCase;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.ScheduleRequest;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.ScheduleResponse;
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
@RequestMapping("/api/v1/teaching-periods/{teachingPeriodId}/schedules")
@Tag(name = "Teaching period schedules")
public class TeachingPeriodScheduleController {

    private final ManageTeachingPeriodScheduleUseCase schedules;

    public TeachingPeriodScheduleController(ManageTeachingPeriodScheduleUseCase schedules) {
        this.schedules = schedules;
    }

    @GetMapping
    @Operation(summary = "Horario semanal de un periodo de enseñanza propio, ordenado por día y hora")
    public List<ScheduleResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long teachingPeriodId) {
        return schedules.list(user.id(), teachingPeriodId).stream().map(ScheduleResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega un bloque de clase; 409 SCHEDULE_CONFLICT si se cruza con otra clase del profesor")
    public ScheduleResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long teachingPeriodId, @Valid @RequestBody ScheduleRequest request) {
        return ScheduleResponse.from(schedules.create(command(user, teachingPeriodId, null, request)));
    }

    @PutMapping("/{scheduleId}")
    @Operation(summary = "Modifica un bloque de clase; 409 SCHEDULE_CONFLICT si se cruza con otra clase del profesor")
    public ScheduleResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                   @PathVariable Long teachingPeriodId, @PathVariable Long scheduleId,
                                   @Valid @RequestBody ScheduleRequest request) {
        return ScheduleResponse.from(schedules.update(command(user, teachingPeriodId, scheduleId, request)));
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un bloque de clase")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long teachingPeriodId,
                       @PathVariable Long scheduleId) {
        schedules.delete(user.id(), teachingPeriodId, scheduleId);
    }

    private static AcademicCommands.SaveSchedule command(AuthenticatedUser user, Long teachingPeriodId,
                                                         Long scheduleId, ScheduleRequest request) {
        return new AcademicCommands.SaveSchedule(user.id(), teachingPeriodId, scheduleId, request.dayOfWeek(),
                request.startTime(), request.endTime(), request.room());
    }
}
