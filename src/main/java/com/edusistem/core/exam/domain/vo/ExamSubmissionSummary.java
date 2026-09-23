package com.edusistem.core.exam.domain.vo;

import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamSubmissionSummary(Long id, Long studentId, String studentCode, String studentName,
                                    ExamSubmissionStatus status, BigDecimal score, BigDecimal finalGrade,
                                    String statusDetail, LocalDateTime submittedAt, LocalDateTime processedAt) {
}
