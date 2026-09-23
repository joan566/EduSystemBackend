package com.edusistem.core.grading.domain.entity;

import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Escala de calificación configurable (0-5, 0-10, 0-100, ...). Nada asume una escala concreta. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingScale {

    private Long id;
    private String name;
    private BigDecimal minimumValue;
    private BigDecimal maximumValue;
    private LocalDateTime createdAt;

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("INVALID_GRADING_SCALE", "The scale name is required");
        }
        if (minimumValue == null || maximumValue == null || minimumValue.compareTo(maximumValue) >= 0) {
            throw new InvalidRequestException("INVALID_GRADING_SCALE", "minimumValue must be lower than maximumValue");
        }
    }

    public boolean contains(BigDecimal value) {
        return value.compareTo(minimumValue) >= 0 && value.compareTo(maximumValue) <= 0;
    }

    /** Convierte una fracción de logro (0..1) a la escala: minimum + fracción × (maximum - minimum). */
    public BigDecimal fromFraction(BigDecimal fraction) {
        BigDecimal clamped = fraction.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        BigDecimal range = maximumValue.subtract(minimumValue);
        return minimumValue.add(clamped.multiply(range)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Convierte una puntuación bruta sobre {@code maximumScore} a la escala. */
    public BigDecimal convert(BigDecimal rawScore, BigDecimal maximumScore) {
        if (maximumScore.signum() <= 0) {
            throw new InvalidRequestException("INVALID_MAXIMUM_SCORE", "maximumScore must be greater than zero");
        }
        return fromFraction(rawScore.divide(maximumScore, MathContext.DECIMAL64));
    }
}
