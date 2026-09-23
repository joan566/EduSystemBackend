package com.edusistem.core.grading.application.use_case.dtos;

import java.math.BigDecimal;
import java.util.List;

public final class GradingCommands {

    private GradingCommands() {
    }

    public record CreateScale(Long actorId, String name, BigDecimal minimumValue, BigDecimal maximumValue) {
    }

    public record WeightInput(Long evaluationCategoryId, BigDecimal weight) {
    }

    public record SaveConfiguration(Long teacherId, Long teachingPeriodId, Long gradingScaleId,
                                    List<WeightInput> weights) {
    }
}
