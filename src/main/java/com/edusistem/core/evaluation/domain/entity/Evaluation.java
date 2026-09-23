package com.edusistem.core.evaluation.domain.entity;

import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cualquier actividad que produce una calificación dentro de un teaching period. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {
    private Long id;
    private Long teachingPeriodId;
    private Long evaluationCategoryId;
    private String name;
    private String description;
    private LocalDateTime evaluationDate;
    private BigDecimal maximumScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void validate() {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("INVALID_EVALUATION", "The evaluation name is required");
        }
        if (maximumScore == null || maximumScore.signum() <= 0) {
            throw new InvalidRequestException("INVALID_MAXIMUM_SCORE", "maximumScore must be greater than zero");
        }
    }

    public void updateDetails(String name, String description, LocalDateTime evaluationDate) {
        this.name = name.trim();
        this.description = description == null || description.isBlank() ? null : description.trim();
        this.evaluationDate = evaluationDate;
    }
}
