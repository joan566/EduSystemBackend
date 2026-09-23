package com.edusistem.core.grading.domain.vo;

import java.math.BigDecimal;

/**
 * Resultado de un estudiante en una evaluación. {@code earned} nulo significa sin dato (cuenta 0);
 * {@code excluded} significa que no debe contar (p. ej. asistencia con excusa o sin registro).
 */
public record EvaluationResult(Long studentId, Long evaluationId, Long categoryId, BigDecimal earned,
                               BigDecimal maximum, boolean excluded) {
}
