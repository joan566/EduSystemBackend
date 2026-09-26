package com.edusistem.core.grading.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.inputports.CalculatePeriodGradeUseCase;
import com.edusistem.core.grading.domain.outputports.EvaluationResultsPort;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.grading.domain.service.PeriodGradeCalculator;
import com.edusistem.core.grading.domain.vo.EvaluationResult;
import com.edusistem.core.grading.domain.vo.PeriodGradeReport;
import com.edusistem.core.grading.domain.vo.StudentPeriodGrade;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PeriodGradeService implements CalculatePeriodGradeUseCase {

    private final GradingConfigurationRepositoryPort configurations;
    private final GradingScaleRepositoryPort scales;
    private final EvaluationResultsPort results;
    private final EvaluationCategoryRepositoryPort categories;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final StudentRepositoryPort students;
    private final OwnershipGuard guard;

    public PeriodGradeService(GradingConfigurationRepositoryPort configurations, GradingScaleRepositoryPort scales,
                              EvaluationResultsPort results, EvaluationCategoryRepositoryPort categories,
                              TeachingPeriodRepositoryPort teachingPeriods, StudentRepositoryPort students,
                              OwnershipGuard guard) {
        this.configurations = configurations;
        this.scales = scales;
        this.results = results;
        this.categories = categories;
        this.teachingPeriods = teachingPeriods;
        this.students = students;
        this.guard = guard;
    }

    @Override
    public PeriodGradeReport calculate(Long teacherId, Long teachingPeriodId) {
        guard.requireTeachingPeriod(teacherId, teachingPeriodId);
        GradingConfiguration configuration = configurations.findByTeachingPeriodId(teachingPeriodId)
                .orElseThrow(() -> new BusinessRuleException("GRADING_CONFIGURATION_REQUIRED",
                        "Configure the grading scale and weights before calculating period grades"));
        configuration.requireComplete();
        GradingScale scale = scales.findById(configuration.getGradingScaleId())
                .orElseThrow(() -> ResourceNotFoundException.of("GradingScale", configuration.getGradingScaleId()));
        TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
        Map<Long, String> categoryNames = categories.findAll().stream()
                .collect(Collectors.toMap(EvaluationCategory::getId, EvaluationCategory::getName));
        Map<Long, List<EvaluationResult>> byStudent = results.findByTeachingPeriodId(teachingPeriodId).stream()
                .collect(Collectors.groupingBy(EvaluationResult::studentId));

        List<StudentPeriodGrade> grades = students.findActiveByGroupId(period.groupId()).stream().map(student -> {
            var result = PeriodGradeCalculator.calculate(configuration, scale,
                    byStudent.getOrDefault(student.getId(), List.of()), categoryNames);
            return new StudentPeriodGrade(student.getId(), student.getStudentCode(), fullName(student),
                    result.categories(), result.periodGrade());
        }).toList();
        return new PeriodGradeReport(teachingPeriodId, scale, grades);
    }

    private static String fullName(Student s) {
        return s.getLastName() + " " + s.getFirstName();
    }
}
