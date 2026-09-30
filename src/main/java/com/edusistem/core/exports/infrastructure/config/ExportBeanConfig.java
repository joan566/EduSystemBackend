package com.edusistem.core.exports.infrastructure.config;

import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.attendance.domain.outputports.AttendanceRecordRepositoryPort;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.exports.application.use_case.service.ExportService;
import com.edusistem.core.exports.application.use_case.service.SchoolSetupExportService;
import com.edusistem.core.grading.domain.inputports.CalculatePeriodGradeUseCase;
import com.edusistem.core.grading.domain.outputports.EvaluationResultsPort;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo exports. */
@Configuration
public class ExportBeanConfig {

    @Bean
    ExportService exportService(StudentRepositoryPort students, TeachingPeriodRepositoryPort teachingPeriods,
                                EvaluationRepositoryPort evaluations, EvaluationResultsPort results,
                                CalculatePeriodGradeUseCase periodGrades, AttendanceSessionRepositoryPort sessions,
                                AttendanceRecordRepositoryPort records, ActivityRepositoryPort activities,
                                GradeActivityUseCase gradeActivity, SpreadsheetWriterPort writer,
                                OwnershipGuard guard, RecordAuditUseCase audit) {
        return new ExportService(students, teachingPeriods, evaluations, results, periodGrades, sessions, records,
                                 activities, gradeActivity, writer, guard, audit);
    }

    @Bean
    SchoolSetupExportService schoolSetupExportService(AcademicPeriodRepositoryPort academicPeriods,
                                                      GradeRepositoryPort grades, SubjectRepositoryPort subjects,
                                                      GroupRepositoryPort groups,
                                                      TeachingPeriodRepositoryPort teachingPeriods,
                                                      GradingConfigurationRepositoryPort configurations,
                                                      GradingScaleRepositoryPort scales,
                                                      EvaluationCategoryRepositoryPort categories,
                                                      TeachingPeriodScheduleRepositoryPort schedules,
                                                      StudentRepositoryPort students,
                                                      ActivityRepositoryPort activities,
                                                      GradeActivityUseCase gradeActivity,
                                                      AttendanceSessionRepositoryPort sessions,
                                                      AttendanceRecordRepositoryPort records,
                                                      SpreadsheetWriterPort writer, RecordAuditUseCase audit) {
        return new SchoolSetupExportService(academicPeriods, grades, subjects, groups, teachingPeriods, configurations,
                                            scales, categories, schedules, students, activities, gradeActivity,
                                            sessions, records, writer, audit);
    }
}
