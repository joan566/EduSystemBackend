package com.edusistem.core.gradebook.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una evaluación de un teaching period con el resultado de un estudiante. {@code earned} nulo = sin nota;
 * {@code excluded} = no cuenta para la nota (asistencia con excusa o sin registro). {@code activityId},
 * {@code examId} y {@code submissionId} permiten ir a la especialización.
 */
public record GradebookRow(Long evaluationId, String name, String description, LocalDateTime evaluationDate,
                           BigDecimal maximumScore, Long categoryId, EvaluationType type, String activityType,
                           BigDecimal earned, boolean excluded, String comment, LocalDateTime gradedAt,
                           Long activityId, Long examId, Long submissionId) {
}
