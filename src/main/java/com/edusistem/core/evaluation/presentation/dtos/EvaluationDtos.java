package com.edusistem.core.evaluation.presentation.dtos;

import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.vo.EvaluationView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class EvaluationDtos {

    private EvaluationDtos() {
    }

    public record UpdateRequest(@NotBlank @Size(max = 150) String name, String description,
                                LocalDateTime evaluationDate) {
    }

    public record CategoryResponse(Long id, String name, String description) {

        public static CategoryResponse from(EvaluationCategory c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
        }
    }

    public record EvaluationResponse(Long id, Long teachingPeriodId, Long categoryId, String categoryName, String name,
                                     String description, LocalDateTime evaluationDate, BigDecimal maximumScore,
                                     Long examId, Long activityId, Long attendanceSessionId) {

        public static EvaluationResponse from(EvaluationView v) {
            return new EvaluationResponse(v.id(), v.teachingPeriodId(), v.categoryId(), v.categoryName(), v.name(),
                    v.description(), v.evaluationDate(), v.maximumScore(), v.examId(), v.activityId(),
                    v.attendanceSessionId());
        }
    }
}
