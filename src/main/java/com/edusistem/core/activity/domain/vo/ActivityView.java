package com.edusistem.core.activity.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActivityView(Long activityId, Long evaluationId, Long teachingPeriodId, String name, String description,
                           LocalDateTime evaluationDate, BigDecimal maximumScore, String activityType) {
}
