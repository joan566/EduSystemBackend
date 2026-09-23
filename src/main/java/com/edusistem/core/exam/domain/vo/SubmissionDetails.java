package com.edusistem.core.exam.domain.vo;

import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.student.domain.entity.Student;
import java.math.BigDecimal;

public record SubmissionDetails(ExamSubmission submission, Student student, Exam exam, String examName,
                                BigDecimal maximumScore, BigDecimal scaleMinimum, BigDecimal scaleMaximum) {
}
