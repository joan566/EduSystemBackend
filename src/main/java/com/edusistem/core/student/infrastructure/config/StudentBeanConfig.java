package com.edusistem.core.student.infrastructure.config;

import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.student.application.use_case.service.StudentService;
import com.edusistem.core.student.domain.outputports.StudentCodeGeneratorPort;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo student. */
@Configuration
public class StudentBeanConfig {

    @Bean
    StudentService studentService(StudentRepositoryPort students, StudentGroupRepositoryPort studentGroups,
                                  StudentCodeGeneratorPort codeGenerator, OwnershipGuard guard,
                                  RecordAuditUseCase audit, Clock clock) {
        return new StudentService(students, studentGroups, codeGenerator, guard, audit, clock);
    }
}
