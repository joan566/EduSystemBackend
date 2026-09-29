package com.edusistem.core.grading.domain.vo;

import com.edusistem.core.grading.domain.entity.GradingScale;
import java.math.BigDecimal;
import java.util.List;

public record PeriodGradeReport(Long teachingPeriodId, GradingScale scale, BigDecimal passingGrade,
                                List<StudentPeriodGrade> students) {
}
