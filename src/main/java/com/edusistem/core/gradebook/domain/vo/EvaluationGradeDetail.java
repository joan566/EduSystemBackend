package com.edusistem.core.gradebook.domain.vo;

import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.gradebook.domain.entity.GradeAttachment;
import com.edusistem.core.student.domain.entity.Student;
import java.util.List;

/** La nota de un estudiante en una evaluación, con su rúbrica (si la hay) y su archivo adjunto (si lo hay). */
public record EvaluationGradeDetail(Long teachingPeriodId, Student student, GradingScale scale, GradebookEntry entry,
                                    List<RubricCriterionScore> rubric, GradeAttachment attachment) {
}
