package com.edusistem.core.attendance.presentation.controllers;

import com.edusistem.core.attendance.application.use_case.dtos.AttendanceCommands;
import com.edusistem.core.attendance.domain.inputports.ManageAttendanceUseCase;
import com.edusistem.core.attendance.presentation.dtos.AttendanceDtos.CreateSessionRequest;
import com.edusistem.core.attendance.presentation.dtos.AttendanceDtos.RecordAttendanceRequest;
import com.edusistem.core.attendance.presentation.dtos.AttendanceDtos.SessionDetailResponse;
import com.edusistem.core.attendance.presentation.dtos.AttendanceDtos.SessionResponse;
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
@RequestMapping("/api/v1/attendance-sessions")
@Tag(name = "Attendance")
public class AttendanceController {

    private final ManageAttendanceUseCase attendance;

    public AttendanceController(ManageAttendanceUseCase attendance) {
        this.attendance = attendance;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una sesión de asistencia (evaluación de categoría ATTENDANCE) para una fecha")
    public SessionResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody CreateSessionRequest request) {
        return SessionResponse.from(attendance.createSession(new AttendanceCommands.CreateSession(user.id(),
                request.teachingPeriodId(), request.sessionDate(), request.name(), request.maximumScore())));
    }

    @GetMapping
    @Operation(summary = "Lista paginada de sesiones de asistencia de un teaching period propio")
    public PageResponse<SessionResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                              @RequestParam Long teachingPeriodId,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        return PageResponse.from(attendance.search(user.id(), teachingPeriodId, PageQuery.of(page, size)),
                SessionResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sesión con el estado de cada estudiante activo del grupo")
    public SessionDetailResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return SessionDetailResponse.from(attendance.get(user.id(), id));
    }

    @PutMapping("/{id}/records")
    @Operation(summary = "Registra o corrige la asistencia (PRESENT, ABSENT, EXCUSED); un registro por estudiante y sesión")
    public SessionDetailResponse record(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                        @Valid @RequestBody RecordAttendanceRequest request) {
        var inputs = request.records().stream()
                .map(r -> new AttendanceCommands.RecordInput(r.studentId(), r.status(), r.observation())).toList();
        return SessionDetailResponse.from(attendance.recordAttendance(
                new AttendanceCommands.RecordAttendance(user.id(), id, inputs)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una sesión sin registros")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        attendance.delete(user.id(), id);
    }
}
