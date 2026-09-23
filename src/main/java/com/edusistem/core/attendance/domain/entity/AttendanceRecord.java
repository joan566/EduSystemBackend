package com.edusistem.core.attendance.domain.entity;

import com.edusistem.core.attendance.domain.enums.AttendanceStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    private Long id;
    private Long attendanceSessionId;
    private Long studentId;
    private AttendanceStatus status;
    private String observation;
    private LocalDateTime createdAt;
}
