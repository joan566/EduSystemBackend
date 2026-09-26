package com.edusistem.core.evaluation.infrastructure.config;

import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.application.use_case.service.EvaluationService;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo evaluation. */
@Configuration
public class EvaluationBeanConfig {

    @Bean
    EvaluationService evaluationService(EvaluationRepositoryPort evaluations,
                                        EvaluationCategoryRepositoryPort categories, OwnershipGuard guard,
                                        RecordAuditUseCase audit) {
        return new EvaluationService(evaluations, categories, guard, audit);
    }
}
