package com.edusistem.core.evaluation.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Evaluación con el identificador de su especialización (solo uno de los tres es no nulo). */
public record EvaluationView(Long id, Long teachingPeriodId, Long categoryId, String categoryName, String name,
                             String description, LocalDateTime evaluationDate, BigDecimal maximumScore, Long examId,
                             Long activityId, Long attendanceSessionId) {
}
