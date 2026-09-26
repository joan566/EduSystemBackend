package com.edusistem.core.activity.infrastructure.config;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.activity.application.use_case.service.ActivityGradeService;
import com.edusistem.core.activity.application.use_case.service.ActivityService;
import com.edusistem.core.activity.domain.outputports.ActivityGradeRepositoryPort;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.inputports.CreateEvaluationUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo activity. */
@Configuration
public class ActivityBeanConfig {

    @Bean
    ActivityGradeService activityGradeService(ActivityRepositoryPort activities, ActivityGradeRepositoryPort grades,
                                              EvaluationRepositoryPort evaluations,
                                              TeachingPeriodRepositoryPort teachingPeriods,
                                              StudentRepositoryPort students,
                                              StudentGroupRepositoryPort studentGroups, OwnershipGuard guard,
                                              RecordAuditUseCase audit, Clock clock) {
        return new ActivityGradeService(activities, grades, evaluations, teachingPeriods, students, studentGroups,
                                        guard, audit, clock);
    }

    @Bean
    ActivityService activityService(ActivityRepositoryPort activities, ActivityGradeRepositoryPort grades,
                                    EvaluationRepositoryPort evaluations, CreateEvaluationUseCase createEvaluation,
                                    OwnershipGuard guard, RecordAuditUseCase audit) {
        return new ActivityService(activities, grades, evaluations, createEvaluation, guard, audit);
    }
}
