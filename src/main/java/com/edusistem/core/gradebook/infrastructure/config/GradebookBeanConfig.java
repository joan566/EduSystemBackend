package com.edusistem.core.gradebook.infrastructure.config;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.gradebook.application.use_case.service.GradeAttachmentService;
import com.edusistem.core.gradebook.application.use_case.service.GradebookService;
import com.edusistem.core.gradebook.application.use_case.service.RubricService;
import com.edusistem.core.gradebook.domain.outputports.GradeAttachmentRepositoryPort;
import com.edusistem.core.gradebook.domain.outputports.GradebookQueryPort;
import com.edusistem.core.gradebook.domain.outputports.RubricRepositoryPort;
import com.edusistem.core.gradebook.domain.outputports.StudentObservationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo gradebook. */
@Configuration
public class GradebookBeanConfig {

    @Bean
    GradebookService gradebookService(GradebookQueryPort query, GradingConfigurationRepositoryPort configurations,
                                      GradingScaleRepositoryPort scales, EvaluationCategoryRepositoryPort categories,
                                      TeachingPeriodRepositoryPort teachingPeriods, StudentRepositoryPort students,
                                      StudentGroupRepositoryPort studentGroups, RubricRepositoryPort rubrics,
                                      GradeAttachmentRepositoryPort attachments,
                                      StudentObservationRepositoryPort observations, OwnershipGuard guard,
                                      RecordAuditUseCase audit) {
        return new GradebookService(query, configurations, scales, categories, teachingPeriods, students,
                studentGroups, rubrics, attachments, observations, guard, audit);
    }

    @Bean
    RubricService rubricService(GradebookQueryPort query, RubricRepositoryPort rubrics,
                                GradeActivityUseCase activityGrades, GradebookService gradebook, OwnershipGuard guard,
                                RecordAuditUseCase audit) {
        return new RubricService(query, rubrics, activityGrades, gradebook, guard, audit);
    }

    @Bean
    GradeAttachmentService gradeAttachmentService(GradebookQueryPort query, GradeAttachmentRepositoryPort attachments,
                                                  GradebookService gradebook, FileStoragePort storage,
                                                  OwnershipGuard guard, RecordAuditUseCase audit) {
        return new GradeAttachmentService(query, attachments, gradebook, storage, guard, audit);
    }
}
