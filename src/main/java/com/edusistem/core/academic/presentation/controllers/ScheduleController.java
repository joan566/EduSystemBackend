package com.edusistem.core.academic.presentation.controllers;

import com.edusistem.core.academic.domain.inputports.QueryTeacherScheduleUseCase;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.ScheduleRangeResponse;
import com.edusistem.core.academic.presentation.dtos.AcademicDtos.TodayScheduleResponse;
import com.edusistem.core.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedule")
@Tag(name = "Schedule")
public class ScheduleController {

    private final QueryTeacherScheduleUseCase schedule;

    public ScheduleController(QueryTeacherScheduleUseCase schedule) {
        this.schedule = schedule;
    }

    @GetMapping("/today")
    @Operation(summary = "Clases del profesor en un día (por defecto hoy, en la zona horaria del colegio)")
    public TodayScheduleResponse today(@AuthenticationPrincipal AuthenticatedUser user,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return TodayScheduleResponse.from(schedule.day(user.id(), date));
    }

    @GetMapping
    @Operation(summary = "Clases del profesor día por día entre from y to (por defecto 7 días desde hoy, máximo 93)")
    public ScheduleRangeResponse range(@AuthenticationPrincipal AuthenticatedUser user,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ScheduleRangeResponse.from(schedule.range(user.id(), from, to));
    }
}
