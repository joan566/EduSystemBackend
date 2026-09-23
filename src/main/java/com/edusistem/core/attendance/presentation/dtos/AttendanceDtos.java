package com.edusistem.core.attendance.presentation.dtos;

import com.edusistem.core.attendance.domain.enums.AttendanceStatus;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionDetails;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.attendance.domain.vo.StudentAttendanceView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    public record CreateSessionRequest(@NotNull Long teachingPeriodId, @NotNull LocalDate sessionDate,
                                       @Size(max = 150) String name, @Positive BigDecimal maximumScore) {
    }

    public record RecordRequest(@NotNull Long studentId, @NotNull AttendanceStatus status,
                                @Size(max = 255) String observation) {
    }

    public record RecordAttendanceRequest(@NotEmpty @Valid List<RecordRequest> records) {
    }

    public record SessionResponse(Long id, Long evaluationId, Long teachingPeriodId, String name, LocalDate sessionDate,
                                  BigDecimal maximumScore) {

        public static SessionResponse from(AttendanceSessionView v) {
            return new SessionResponse(v.sessionId(), v.evaluationId(), v.teachingPeriodId(), v.name(), v.sessionDate(),
                    v.maximumScore());
        }
    }

    public record StudentAttendanceResponse(Long studentId, String studentCode, String studentName, String status,
                                            String observation) {

        static StudentAttendanceResponse from(StudentAttendanceView v) {
            return new StudentAttendanceResponse(v.studentId(), v.studentCode(), v.studentName(),
                    v.status() == null ? null : v.status().name(), v.observation());
        }
    }

    public record SessionDetailResponse(SessionResponse session, List<StudentAttendanceResponse> students) {

        public static SessionDetailResponse from(AttendanceSessionDetails d) {
            return new SessionDetailResponse(SessionResponse.from(d.session()),
                    d.students().stream().map(StudentAttendanceResponse::from).toList());
        }
    }
}
