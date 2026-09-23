package com.edusistem.core.attendance.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una sesión (fecha) de asistencia; es la especialización de una Evaluation de categoría ATTENDANCE. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession {
    private Long id;
    private Long evaluationId;
    private LocalDate sessionDate;
    private LocalDateTime createdAt;
}
