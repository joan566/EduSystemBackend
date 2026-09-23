package com.edusistem.core.exam.domain.vo;

import com.edusistem.core.exam.domain.entity.Exam;

public record ExamDetails(ExamView summary, Exam exam) {
}
