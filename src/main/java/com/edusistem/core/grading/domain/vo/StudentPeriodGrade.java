package com.edusistem.core.grading.domain.vo;

import java.math.BigDecimal;
import java.util.List;

public record StudentPeriodGrade(Long studentId, String studentCode, String studentName,
                                 List<CategoryBreakdown> categories, BigDecimal periodGrade) {
}
