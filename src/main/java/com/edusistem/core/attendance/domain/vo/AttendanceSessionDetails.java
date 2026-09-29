package com.edusistem.core.attendance.domain.vo;

import java.util.List;

/** {@code session} es nulo sólo en la asistencia de un día que aún no tiene sesión. */
public record AttendanceSessionDetails(AttendanceSessionView session, List<StudentAttendanceView> students) {
}
