package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GradingScaleTest {

    private static GradingScale scale(String min, String max) {
        return GradingScale.builder().name("s").minimumValue(new BigDecimal(min)).maximumValue(new BigDecimal(max)).build();
    }

    @Test
    void convertsRawScoreToScale0to5() {
        // 17 de 20 preguntas: la puntuación bruta (17) NO es la nota (4.25)
        assertThat(scale("0", "5").convert(new BigDecimal("17"), new BigDecimal("20"))).isEqualByComparingTo("4.25");
    }

    @Test
    void convertsRawScoreToScale0to10() {
        assertThat(scale("0", "10").convert(new BigDecimal("17"), new BigDecimal("20"))).isEqualByComparingTo("8.50");
    }

    @Test
    void convertsRawScoreToScale0to100() {
        assertThat(scale("0", "100").convert(new BigDecimal("17"), new BigDecimal("20"))).isEqualByComparingTo("85.00");
    }

    @Test
    void respectsNonZeroMinimum() {
        assertThat(scale("1", "7").convert(new BigDecimal("10"), new BigDecimal("20"))).isEqualByComparingTo("4.00");
        assertThat(scale("1", "7").convert(BigDecimal.ZERO, new BigDecimal("20"))).isEqualByComparingTo("1.00");
    }

    @Test
    void containsChecksBounds() {
        GradingScale s = scale("0", "5");
        assertThat(s.contains(new BigDecimal("5.00"))).isTrue();
        assertThat(s.contains(new BigDecimal("0"))).isTrue();
        assertThat(s.contains(new BigDecimal("5.01"))).isFalse();
        assertThat(s.contains(new BigDecimal("-0.1"))).isFalse();
    }

    @Test
    void rejectsInvalidScale() {
        assertThatThrownBy(() -> scale("5", "5").validate()).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> scale("10", "0").validate()).isInstanceOf(InvalidRequestException.class);
    }
}
