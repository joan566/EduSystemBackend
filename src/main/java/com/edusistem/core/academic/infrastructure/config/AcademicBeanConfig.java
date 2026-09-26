package com.edusistem.core.academic.infrastructure.config;

import com.edusistem.core.academic.application.use_case.service.AcademicPeriodService;
import com.edusistem.core.academic.application.use_case.service.GradeService;
import com.edusistem.core.academic.application.use_case.service.GroupService;
import com.edusistem.core.academic.application.use_case.service.TeachingAssignmentService;
import com.edusistem.core.academic.application.use_case.service.TeachingPeriodService;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingAssignmentRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.outputports.CatalogUsagePort;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo academic. */
@Configuration
public class AcademicBeanConfig {

    @Bean
    AcademicPeriodService academicPeriodService(AcademicPeriodRepositoryPort periods, CatalogUsagePort usage,
                                                RecordAuditUseCase audit) {
        return new AcademicPeriodService(periods, usage, audit);
    }

    @Bean
    GradeService gradeService(GradeRepositoryPort grades, CatalogUsagePort usage, RecordAuditUseCase audit) {
        return new GradeService(grades, usage, audit);
    }

    @Bean
    GroupService groupService(GroupRepositoryPort groups, GradeRepositoryPort grades, CatalogUsagePort usage,
                              RecordAuditUseCase audit) {
        return new GroupService(groups, grades, usage, audit);
    }

    @Bean
    TeachingAssignmentService teachingAssignmentService(TeachingAssignmentRepositoryPort assignments,
                                                        GroupRepositoryPort groups, SubjectRepositoryPort subjects,
                                                        OwnershipGuard guard, RecordAuditUseCase audit) {
        return new TeachingAssignmentService(assignments, groups, subjects, guard, audit);
    }

    @Bean
    TeachingPeriodService teachingPeriodService(TeachingPeriodRepositoryPort teachingPeriods,
                                                TeachingAssignmentRepositoryPort assignments,
                                                AcademicPeriodRepositoryPort academicPeriods, OwnershipGuard guard,
                                                RecordAuditUseCase audit) {
        return new TeachingPeriodService(teachingPeriods, assignments, academicPeriods, guard, audit);
    }
}
