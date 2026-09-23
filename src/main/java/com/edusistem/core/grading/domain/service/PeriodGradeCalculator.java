package com.edusistem.core.grading.domain.service;

import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.domain.vo.CategoryBreakdown;
import com.edusistem.core.grading.domain.vo.EvaluationResult;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Nota del periodo = Σ (peso_categoría / 100 × logro_categoría), llevada a la escala configurada.
 * Logro de categoría = Σ obtenido / Σ máximo de las evaluaciones de la categoría (sin dato = 0, excluidas no cuentan).
 * No conoce porcentajes concretos: todo sale de grading_weights. Es independiente de la nota final de un examen.
 */
public final class PeriodGradeCalculator {

    private PeriodGradeCalculator() {
    }

    public record Result(List<CategoryBreakdown> categories, BigDecimal periodGrade) {
    }

    public static Result calculate(GradingConfiguration configuration, GradingScale scale,
                                   List<EvaluationResult> studentResults, Map<Long, String> categoryNames) {
        BigDecimal periodFraction = BigDecimal.ZERO;
        List<CategoryBreakdown> breakdown = new ArrayList<>();
        for (GradingWeight weight : configuration.getWeights()) {
            List<EvaluationResult> inCategory = studentResults.stream()
                    .filter(r -> r.categoryId().equals(weight.getEvaluationCategoryId()) && !r.excluded()).toList();
            BigDecimal earned = inCategory.stream().map(r -> r.earned() == null ? BigDecimal.ZERO : r.earned())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal maximum = inCategory.stream().map(EvaluationResult::maximum).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal achievement = maximum.signum() == 0 ? BigDecimal.ZERO
                    : earned.divide(maximum, MathContext.DECIMAL64).min(BigDecimal.ONE);
            periodFraction = periodFraction.add(achievement.multiply(
                    weight.getWeight().divide(GradingConfiguration.HUNDRED, MathContext.DECIMAL64)));
            breakdown.add(new CategoryBreakdown(weight.getEvaluationCategoryId(),
                    categoryNames.getOrDefault(weight.getEvaluationCategoryId(), "?"), weight.getWeight(),
                    achievement.setScale(4, java.math.RoundingMode.HALF_UP), scale.fromFraction(achievement),
                    inCategory.size()));
        }
        return new Result(breakdown, scale.fromFraction(periodFraction));
    }
}
