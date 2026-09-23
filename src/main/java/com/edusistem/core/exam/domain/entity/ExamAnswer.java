package com.edusistem.core.exam.domain.entity;

import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
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
public class ExamAnswer {
    private Long id;
    private Long submissionId;
    private Long questionId;
    /** Opción elegida; nula si está vacía o hay marca múltiple. */
    private String selectedOption;
    /** Nulo cuando la pregunta no se pudo evaluar (vacía, múltiple o pendiente de revisión). */
    private Boolean correct;
    private BigDecimal detectionConfidence;
    private AnswerDetectionStatus detectionStatus;
    private LocalDateTime createdAt;

    public boolean needsReview() {
        return detectionStatus == AnswerDetectionStatus.MULTIPLE_MARK || detectionStatus == AnswerDetectionStatus.REVIEW_REQUIRED;
    }
}
