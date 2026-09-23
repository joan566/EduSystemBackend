package com.edusistem.core.attendance.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceSessionView(Long sessionId, Long evaluationId, Long teachingPeriodId, String name,
                                    LocalDate sessionDate, BigDecimal maximumScore) {
}
