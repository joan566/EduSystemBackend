package com.edusistem.core.gradebook.domain.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Criterio de la rúbrica de una evaluación; cada estudiante se califica en él sobre el puntaje máximo. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RubricCriterion {
    private Long id;
    private Long evaluationId;
    private int position;
    private String name;
    /** Porcentaje de la nota de la evaluación (los criterios suman 100). */
    private BigDecimal weight;
}
