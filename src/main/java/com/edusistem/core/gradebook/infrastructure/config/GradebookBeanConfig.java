package com.edusistem.core.gradebook.infrastructure.config;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.gradebook.application.contracts.EnrolledStudentLookup;
import com.edusistem.core.gradebook.application.enrollment.DefaultEnrolledStudentLookup;
import com.edusistem.core.gradebook.application.use_case.service.GradeAttachmentService;
import com.edusistem.core.gradebook.application.use_case.service.GradebookService;
import com.edusistem.core.gradebook.application.use_case.service.RubricService;
import com.edusistem.core.gradebook.application.use_case.service.StudentObservationService;
import com.edusistem.core.gradebook.domain.inputports.QueryGradebookUseCase;
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
    EnrolledStudentLookup enrolledStudentLookup(TeachingPeriodRepositoryPort teachingPeriods,
                                                StudentRepositoryPort students,
                                                StudentGroupRepositoryPort studentGroups) {
        return new DefaultEnrolledStudentLookup(teachingPeriods, students, studentGroups);
    }

    @Bean
    GradebookService gradebookService(GradebookQueryPort query, GradingConfigurationRepositoryPort configurations,
                                      GradingScaleRepositoryPort scales, EvaluationCategoryRepositoryPort categories,
                                      RubricRepositoryPort rubrics, GradeAttachmentRepositoryPort attachments,
                                      StudentObservationRepositoryPort observations,
                                      EnrolledStudentLookup enrolledStudents, OwnershipGuard guard) {
        return new GradebookService(query, configurations, scales, categories, rubrics, attachments, observations,
                enrolledStudents, guard);
    }

    @Bean
    StudentObservationService studentObservationService(StudentObservationRepositoryPort observations,
                                                        EnrolledStudentLookup enrolledStudents, OwnershipGuard guard,
                                                        RecordAuditUseCase audit) {
        return new StudentObservationService(observations, enrolledStudents, guard, audit);
    }

    @Bean
    RubricService rubricService(GradebookQueryPort query, RubricRepositoryPort rubrics,
                                GradeActivityUseCase activityGrades, QueryGradebookUseCase gradebook, OwnershipGuard guard,
                                RecordAuditUseCase audit) {
        return new RubricService(query, rubrics, activityGrades, gradebook, guard, audit);
    }

    @Bean
    GradeAttachmentService gradeAttachmentService(GradebookQueryPort query, GradeAttachmentRepositoryPort attachments,
                                                  EnrolledStudentLookup enrolledStudents, FileStoragePort storage,
                                                  OwnershipGuard guard, RecordAuditUseCase audit) {
        return new GradeAttachmentService(query, attachments, enrolledStudents, storage, guard, audit);
    }
}
