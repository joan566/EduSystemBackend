package com.edusistem.core.grading.domain.entity;

import java.math.BigDecimal;
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
public class GradingWeight {
    private Long id;
    private Long evaluationCategoryId;
    /** Porcentaje 0-100. */
    private BigDecimal weight;
}
