package com.edusistem.core.gradebook.domain.vo;

import com.edusistem.core.gradebook.domain.entity.RubricCriterion;
import java.math.BigDecimal;

/** Un criterio de la rúbrica con el puntaje del estudiante (nulo si aún no se ha calificado con rúbrica). */
public record RubricCriterionScore(RubricCriterion criterion, BigDecimal score) {
}
