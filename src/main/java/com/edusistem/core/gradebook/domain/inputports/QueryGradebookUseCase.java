package com.edusistem.core.gradebook.domain.inputports;

import com.edusistem.core.gradebook.domain.vo.EvaluationGradeDetail;
import com.edusistem.core.gradebook.domain.vo.StudentGradeReport;

public interface QueryGradebookUseCase {

    StudentGradeReport studentReport(Long teacherId, Long teachingPeriodId, Long studentId);

    EvaluationGradeDetail gradeDetail(Long teacherId, Long evaluationId, Long studentId);
}
