package com.edusistem.core.activity.domain.entity;

import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGrade {
    private Long id;
    private Long activityId;
    private Long studentId;
    private BigDecimal grade;
    private String comment;
    private LocalDateTime gradedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** La nota debe estar entre 0 y el puntaje máximo de la evaluación. */
    public static void validateGrade(BigDecimal grade, BigDecimal maximumScore) {
        if (grade == null || grade.signum() < 0 || grade.compareTo(maximumScore) > 0) {
            throw new InvalidRequestException("GRADE_OUT_OF_RANGE",
                    "The grade must be between 0 and " + maximumScore.stripTrailingZeros().toPlainString());
        }
    }
}
