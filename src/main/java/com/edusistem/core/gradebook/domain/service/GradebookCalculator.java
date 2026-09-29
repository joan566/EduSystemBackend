package com.edusistem.core.gradebook.domain.service;

import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.domain.service.PeriodGradeCalculator;
import com.edusistem.core.grading.domain.vo.CategoryBreakdown;
import com.edusistem.core.grading.domain.vo.EvaluationResult;
import com.edusistem.core.gradebook.domain.vo.GradebookEntry;
import com.edusistem.core.gradebook.domain.vo.GradebookRow;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lleva el cálculo de la nota del periodo ({@link PeriodGradeCalculator}) al nivel de cada evaluación.
 * En una categoría con peso W, cuyas evaluaciones que cuentan suman S puntos máximos, una evaluación con máximo m
 * pesa W × m / S y aporta W × obtenido / S puntos (sobre 100). La suma de los aportes es la nota del periodo en
 * puntos, así que ambos cálculos siempre coinciden.
 */
public final class GradebookCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private GradebookCalculator() {
    }

    public record Result(List<CategoryBreakdown> categories, BigDecimal periodGrade, BigDecimal score,
                         List<GradebookEntry> entries) {
    }

    /**
     * @param configuration nula si la clase no está configurada; solo si está completa hay nota y aportes
     */
    public static Result calculate(Long studentId, List<GradebookRow> rows, GradingConfiguration configuration,
                                   GradingScale scale, Map<Long, String> categoryNames, Set<Long> withRubric,
                                   Set<Long> withAttachment) {
        boolean complete = configuration != null && configuration.isComplete() && scale != null;
        Map<Long, BigDecimal> weights = configuration == null ? Map.of() : configuration.getWeights().stream()
                .collect(Collectors.toMap(GradingWeight::getEvaluationCategoryId, GradingWeight::getWeight));
        Map<Long, BigDecimal> maxByCategory = rows.stream().filter(r -> !r.excluded())
                .collect(Collectors.groupingBy(GradebookRow::categoryId,
                        Collectors.reducing(BigDecimal.ZERO, GradebookRow::maximumScore, BigDecimal::add)));

        List<GradebookEntry> entries = rows.stream().map(r -> {
            BigDecimal weight = null;
            BigDecimal contribution = null;
            if (configuration != null) {
                BigDecimal categoryWeight = weights.getOrDefault(r.categoryId(), BigDecimal.ZERO);
                BigDecimal categoryMax = maxByCategory.getOrDefault(r.categoryId(), BigDecimal.ZERO);
                if (r.excluded() || categoryMax.signum() == 0) {
                    weight = BigDecimal.ZERO;
                } else {
                    weight = share(categoryWeight, r.maximumScore(), categoryMax);
                    if (complete) {
                        contribution = share(categoryWeight,
                                r.earned() == null ? BigDecimal.ZERO : r.earned().min(r.maximumScore()), categoryMax);
                    }
                }
            }
            return new GradebookEntry(r, categoryNames.getOrDefault(r.categoryId(), "?"), weight, contribution,
                    withRubric.contains(r.evaluationId()), withAttachment.contains(r.evaluationId()));
        }).toList();

        if (!complete) {
            return new Result(List.of(), null, null, entries);
        }
        List<EvaluationResult> results = rows.stream().map(r -> new EvaluationResult(studentId, r.evaluationId(),
                r.categoryId(), r.earned(), r.maximumScore(), r.excluded())).toList();
        PeriodGradeCalculator.Result period = PeriodGradeCalculator.calculate(configuration, scale, results,
                categoryNames);
        BigDecimal score = period.categories().stream()
                .map(c -> c.achievement().multiply(c.weight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new Result(period.categories(), period.periodGrade(), score, entries);
    }

    /** W × part / total, en puntos sobre 100 con dos decimales. */
    private static BigDecimal share(BigDecimal categoryWeight, BigDecimal part, BigDecimal total) {
        return categoryWeight.multiply(part).divide(total, MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
    }

    /** Nota de una evaluación calificada con rúbrica: Σ puntaje × peso / 100 (puntajes sobre el máximo). */
    public static BigDecimal rubricGrade(List<BigDecimal> scores, List<BigDecimal> weights) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < scores.size(); i++) {
            total = total.add(scores.get(i).multiply(weights.get(i)));
        }
        return total.divide(HUNDRED, MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
    }
}
