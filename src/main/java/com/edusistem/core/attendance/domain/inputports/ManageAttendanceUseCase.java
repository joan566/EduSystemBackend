package com.edusistem.core.attendance.domain.inputports;

import com.edusistem.core.attendance.application.use_case.dtos.AttendanceCommands;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionDetails;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.time.LocalDate;

public interface ManageAttendanceUseCase {

    AttendanceSessionView createSession(AttendanceCommands.CreateSession command);

    AttendanceSessionDetails get(Long teacherId, Long sessionId);

    /** Registra o corrige el estado de varios estudiantes (uno por estudiante y sesión). */
    AttendanceSessionDetails recordAttendance(AttendanceCommands.RecordAttendance command);

    /** Solo si aún no tiene registros. */
    void delete(Long teacherId, Long sessionId);

    /**
     * El grupo activo de la clase con su asistencia de {@code date}: la de la sesión de ese día si existe (la más
     * reciente si hay varias) o, si no, cada estudiante sin estado y {@code session} nulo.
     */
    AttendanceSessionDetails getDay(Long teacherId, Long teachingPeriodId, LocalDate date);

    PageResult<AttendanceSessionView> search(Long teacherId, Long teachingPeriodId, PageQuery page);
}
