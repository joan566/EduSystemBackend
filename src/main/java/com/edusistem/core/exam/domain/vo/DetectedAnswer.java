package com.edusistem.core.exam.domain.vo;

import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
import java.math.BigDecimal;

public record DetectedAnswer(int questionNumber, String selectedOption, AnswerDetectionStatus status,
                             BigDecimal confidence) {
}
