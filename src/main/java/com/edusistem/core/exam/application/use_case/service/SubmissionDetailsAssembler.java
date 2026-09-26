package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.exam.domain.outputports.ExamRepositoryPort;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;

public class SubmissionDetailsAssembler {

    private final ExamRepositoryPort exams;
    private final EvaluationRepositoryPort evaluations;
    private final StudentRepositoryPort students;
    private final ExamContextLoader loader;

    public SubmissionDetailsAssembler(ExamRepositoryPort exams, EvaluationRepositoryPort evaluations,
                               StudentRepositoryPort students, ExamContextLoader loader) {
        this.exams = exams;
        this.evaluations = evaluations;
        this.students = students;
        this.loader = loader;
    }

    SubmissionDetails assemble(ExamSubmission submission) {
        Exam exam = exams.findById(submission.getExamId())
                .orElseThrow(() -> ResourceNotFoundException.of("Exam", submission.getExamId()));
        Evaluation evaluation = evaluations.findById(exam.getEvaluationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation", exam.getEvaluationId()));
        Student student = students.findById(submission.getStudentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Student", submission.getStudentId()));
        GradingScale scale = loader.scaleOrNull(evaluation.getTeachingPeriodId());
        return new SubmissionDetails(submission, student, exam, evaluation.getName(), evaluation.getMaximumScore(),
                scale == null ? null : scale.getMinimumValue(), scale == null ? null : scale.getMaximumValue());
    }
}
