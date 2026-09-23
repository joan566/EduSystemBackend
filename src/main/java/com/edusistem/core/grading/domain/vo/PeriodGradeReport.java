package com.edusistem.core.grading.domain.vo;

import com.edusistem.core.grading.domain.entity.GradingScale;
import java.util.List;

public record PeriodGradeReport(Long teachingPeriodId, GradingScale scale, List<StudentPeriodGrade> students) {
}
