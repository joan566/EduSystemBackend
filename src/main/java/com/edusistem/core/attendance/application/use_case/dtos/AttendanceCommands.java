package com.edusistem.core.attendance.application.use_case.dtos;

import com.edusistem.core.attendance.domain.enums.AttendanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceCommands {

    private AttendanceCommands() {
    }

    /** {@code name} y {@code maximumScore} son opcionales (por defecto "Asistencia <fecha>" y 1). */
    public record CreateSession(Long teacherId, Long teachingPeriodId, LocalDate sessionDate, String name,
                                BigDecimal maximumScore) {
    }

    public record RecordInput(Long studentId, AttendanceStatus status, String observation) {
    }

    public record RecordAttendance(Long teacherId, Long sessionId, List<RecordInput> records) {
    }
}
