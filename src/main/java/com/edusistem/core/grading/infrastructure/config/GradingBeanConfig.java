package com.edusistem.core.grading.infrastructure.config;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.grading.application.use_case.service.GradingConfigurationService;
import com.edusistem.core.grading.application.use_case.service.GradingScaleService;
import com.edusistem.core.grading.application.use_case.service.PeriodGradeService;
import com.edusistem.core.grading.domain.outputports.EvaluationResultsPort;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo grading. */
@Configuration
public class GradingBeanConfig {

    @Bean
    GradingConfigurationService gradingConfigurationService(GradingConfigurationRepositoryPort configurations,
                                                            GradingScaleRepositoryPort scales,
                                                            EvaluationCategoryRepositoryPort categories,
                                                            EvaluationResultsPort results, OwnershipGuard guard,
                                                            RecordAuditUseCase audit) {
        return new GradingConfigurationService(configurations, scales, categories, results, guard, audit);
    }

    @Bean
    GradingScaleService gradingScaleService(GradingScaleRepositoryPort scales, RecordAuditUseCase audit) {
        return new GradingScaleService(scales, audit);
    }

    @Bean
    PeriodGradeService periodGradeService(GradingConfigurationRepositoryPort configurations,
                                          GradingScaleRepositoryPort scales, EvaluationResultsPort results,
                                          EvaluationCategoryRepositoryPort categories,
                                          TeachingPeriodRepositoryPort teachingPeriods,
                                          StudentRepositoryPort students, OwnershipGuard guard) {
        return new PeriodGradeService(configurations, scales, results, categories, teachingPeriods, students, guard);
    }
}
