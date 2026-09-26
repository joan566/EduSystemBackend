package com.edusistem.core.attendance.infrastructure.config;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.attendance.application.use_case.service.AttendanceService;
import com.edusistem.core.attendance.domain.outputports.AttendanceRecordRepositoryPort;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.inputports.CreateEvaluationUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo attendance. */
@Configuration
public class AttendanceBeanConfig {

    @Bean
    AttendanceService attendanceService(AttendanceSessionRepositoryPort sessions,
                                        AttendanceRecordRepositoryPort records, EvaluationRepositoryPort evaluations,
                                        CreateEvaluationUseCase createEvaluation,
                                        TeachingPeriodRepositoryPort teachingPeriods, StudentRepositoryPort students,
                                        StudentGroupRepositoryPort studentGroups, OwnershipGuard guard,
                                        RecordAuditUseCase audit) {
        return new AttendanceService(sessions, records, evaluations, createEvaluation, teachingPeriods, students,
                                     studentGroups, guard, audit);
    }
}
