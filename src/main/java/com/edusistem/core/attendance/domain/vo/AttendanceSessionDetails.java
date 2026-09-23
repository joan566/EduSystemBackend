package com.edusistem.core.attendance.domain.vo;

import java.util.List;

public record AttendanceSessionDetails(AttendanceSessionView session, List<StudentAttendanceView> students) {
}
