package com.edusistem.core.attendance.domain.vo;

import com.edusistem.core.attendance.domain.enums.AttendanceStatus;

/** Estado de un estudiante activo del grupo en la sesión; {@code status} es nulo si aún no se registró. */
public record StudentAttendanceView(Long studentId, String studentCode, String studentName, AttendanceStatus status,
                                    String observation) {
}
