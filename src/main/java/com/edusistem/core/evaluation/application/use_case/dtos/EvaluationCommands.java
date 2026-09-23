package com.edusistem.core.evaluation.application.use_case.dtos;

import com.edusistem.core.evaluation.domain.enums.EvaluationCategoryCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class EvaluationCommands {

    private EvaluationCommands() {
    }

    public record Create(Long teacherId, Long teachingPeriodId, EvaluationCategoryCode category, String name,
                         String description, LocalDateTime evaluationDate, BigDecimal maximumScore) {
    }

    public record UpdateDetails(Long teacherId, Long evaluationId, String name, String description,
                                LocalDateTime evaluationDate) {
    }
}
