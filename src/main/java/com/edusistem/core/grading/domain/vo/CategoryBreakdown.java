package com.edusistem.core.grading.domain.vo;

import java.math.BigDecimal;

/** Desglose de una categoría: fracción de logro (0..1), su equivalente en la escala y evaluaciones contadas. */
public record CategoryBreakdown(Long categoryId, String categoryName, BigDecimal weight, BigDecimal achievement,
                                BigDecimal gradeOnScale, int evaluationCount) {
}
