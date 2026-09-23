package com.edusistem.core.exam.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamView(Long examId, Long evaluationId, Long teachingPeriodId, String name, String description,
                       LocalDateTime evaluationDate, BigDecimal maximumScore, int numberOfQuestions) {
}
