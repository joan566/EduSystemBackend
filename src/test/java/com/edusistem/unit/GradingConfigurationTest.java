package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.domain.service.PeriodGradeCalculator;
import com.edusistem.core.grading.domain.vo.EvaluationResult;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GradingConfigurationTest {

    private static GradingConfiguration config(double... weights) {
        List<GradingWeight> list = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) {
            list.add(GradingWeight.builder().evaluationCategoryId((long) (i + 1)).weight(BigDecimal.valueOf(weights[i])).build());
        }
        return GradingConfiguration.builder().weights(list).build();
    }

    @Test
    void weightsSummingTo100AreValidAndComplete() {
        GradingConfiguration c = config(60, 20, 20);
        assertThatCode(c::validateForSave).doesNotThrowAnyException();
        assertThat(c.isComplete()).isTrue();
        assertThatCode(c::requireComplete).doesNotThrowAnyException();
    }

    @Test
    void weightsExceeding100AreRejected() {
        assertThatThrownBy(() -> config(60, 20, 30).validateForSave())
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("110");
    }

    @Test
    void partialWeightsCanBeSavedButCannotBeUsedToCalculate() {
        GradingConfiguration c = config(60, 20);
        assertThatCode(c::validateForSave).doesNotThrowAnyException();
        assertThat(c.isComplete()).isFalse();
        assertThatThrownBy(c::requireComplete).isInstanceOf(BusinessRuleException.class).hasMessageContaining("80");
    }

    @Test
    void negativeOrDuplicateWeightsAreRejected() {
        assertThatThrownBy(() -> config(-5, 50).validateForSave()).isInstanceOf(InvalidRequestException.class);
        GradingConfiguration duplicated = config(50, 50);
        duplicated.getWeights().get(1).setEvaluationCategoryId(1L);
        assertThatThrownBy(duplicated::validateForSave).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void periodGradeIsWeightedAndRespectsTheScale() {
        GradingConfiguration c = config(60, 20, 20);
        // logro: exámenes 80%, actividades 100%, asistencia 50%
        List<EvaluationResult> results = List.of(
                new EvaluationResult(1L, 10L, 1L, new BigDecimal("16"), new BigDecimal("20"), false),
                new EvaluationResult(1L, 11L, 2L, new BigDecimal("5"), new BigDecimal("5"), false),
                new EvaluationResult(1L, 12L, 3L, BigDecimal.ONE, BigDecimal.ONE, false),
                new EvaluationResult(1L, 13L, 3L, BigDecimal.ZERO, BigDecimal.ONE, false));
        Map<Long, String> names = Map.of(1L, "EXAMS", 2L, "ACTIVITIES", 3L, "ATTENDANCE");
        // 0.6*0.8 + 0.2*1.0 + 0.2*0.5 = 0.78
        GradingScale s5 = GradingScale.builder().minimumValue(BigDecimal.ZERO).maximumValue(new BigDecimal("5")).build();
        GradingScale s100 = GradingScale.builder().minimumValue(BigDecimal.ZERO).maximumValue(new BigDecimal("100")).build();
        assertThat(PeriodGradeCalculator.calculate(c, s5, results, names).periodGrade()).isEqualByComparingTo("3.90");
        assertThat(PeriodGradeCalculator.calculate(c, s100, results, names).periodGrade()).isEqualByComparingTo("78.00");
    }

    @Test
    void excludedResultsDoNotCountAndMissingGradesCountAsZero() {
        GradingConfiguration c = config(50, 50);
        List<EvaluationResult> results = List.of(
                new EvaluationResult(1L, 10L, 1L, null, new BigDecimal("10"), false),          // sin nota => 0
                new EvaluationResult(1L, 11L, 2L, null, BigDecimal.ONE, true),                 // excusa => ignorada
                new EvaluationResult(1L, 12L, 2L, BigDecimal.ONE, BigDecimal.ONE, false));
        GradingScale s10 = GradingScale.builder().minimumValue(BigDecimal.ZERO).maximumValue(BigDecimal.TEN).build();
        var result = PeriodGradeCalculator.calculate(c, s10, results, Map.of(1L, "EXAMS", 2L, "ATTENDANCE"));
        assertThat(result.periodGrade()).isEqualByComparingTo("5.00"); // 0.5*0 + 0.5*1
        assertThat(result.categories().get(1).evaluationCount()).isEqualTo(1);
    }
}
