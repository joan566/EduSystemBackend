package com.edusistem.core.grading.domain.entity;

import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuración de calificación de un teaching period. Política de pesos:
 * <ul>
 *   <li>Guardar: cada peso entre 0 y 100, sin categorías repetidas y suma ≤ 100 (se permiten configuraciones parciales).</li>
 *   <li>Calcular una nota del periodo: la suma debe ser exactamente 100.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingConfiguration {

    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private Long id;
    private Long teachingPeriodId;
    private Long gradingScaleId;
    @Builder.Default
    private List<GradingWeight> weights = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BigDecimal totalWeight() {
        return weights.stream().map(GradingWeight::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isComplete() {
        return totalWeight().compareTo(HUNDRED) == 0;
    }

    /** Validación para guardar (permite parciales). */
    public void validateForSave() {
        Set<Long> seen = new HashSet<>();
        for (GradingWeight w : weights) {
            if (w.getWeight() == null || w.getWeight().signum() < 0 || w.getWeight().compareTo(HUNDRED) > 0) {
                throw new InvalidRequestException("INVALID_WEIGHT", "Each weight must be between 0 and 100");
            }
            if (!seen.add(w.getEvaluationCategoryId())) {
                throw new InvalidRequestException("DUPLICATE_WEIGHT_CATEGORY",
                        "Each evaluation category can appear only once");
            }
        }
        if (totalWeight().compareTo(HUNDRED) > 0) {
            throw new InvalidRequestException("WEIGHTS_EXCEED_100",
                    "The weights add up to " + totalWeight().stripTrailingZeros().toPlainString() + "%, which exceeds 100%");
        }
    }

    /** Validación para calcular notas: exige suma exacta de 100%. */
    public void requireComplete() {
        if (!isComplete()) {
            throw new BusinessRuleException("GRADING_CONFIGURATION_INCOMPLETE",
                    "The weights must add up to 100% (currently " + totalWeight().stripTrailingZeros().toPlainString() + "%)");
        }
    }
}
