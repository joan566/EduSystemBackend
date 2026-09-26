package com.edusistem.core.subject.infrastructure.config;

import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.domain.outputports.CatalogUsagePort;
import com.edusistem.core.subject.application.use_case.service.SubjectService;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo subject. */
@Configuration
public class SubjectBeanConfig {

    @Bean
    SubjectService subjectService(SubjectRepositoryPort subjects, CatalogUsagePort usage, RecordAuditUseCase audit) {
        return new SubjectService(subjects, usage, audit);
    }
}
