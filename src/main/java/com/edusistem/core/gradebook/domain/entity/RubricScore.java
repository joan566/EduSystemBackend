package com.edusistem.core.gradebook.domain.entity;

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
public class RubricScore {
    private Long id;
    private Long criterionId;
    private Long studentId;
    private BigDecimal score;
}
