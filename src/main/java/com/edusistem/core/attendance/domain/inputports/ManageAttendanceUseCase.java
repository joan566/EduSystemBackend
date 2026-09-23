package com.edusistem.core.attendance.domain.inputports;

import com.edusistem.core.attendance.application.use_case.dtos.AttendanceCommands;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionDetails;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface ManageAttendanceUseCase {

    AttendanceSessionView createSession(AttendanceCommands.CreateSession command);

    AttendanceSessionDetails get(Long teacherId, Long sessionId);

    /** Registra o corrige el estado de varios estudiantes (uno por estudiante y sesión). */
    AttendanceSessionDetails recordAttendance(AttendanceCommands.RecordAttendance command);

    /** Solo si aún no tiene registros. */
    void delete(Long teacherId, Long sessionId);

    PageResult<AttendanceSessionView> search(Long teacherId, Long teachingPeriodId, PageQuery page);
}
