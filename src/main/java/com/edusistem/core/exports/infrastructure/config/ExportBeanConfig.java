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
import com.edusistem.core.exports.application.contracts.SchoolSetupSheetExporter;
import com.edusistem.core.exports.application.contracts.SchoolSetupSnapshotLoader;
import com.edusistem.core.exports.application.schoolsetup.DefaultSchoolSetupSnapshotLoader;
import com.edusistem.core.exports.application.schoolsetup.sheets.AcademicGradesSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.AcademicPeriodsSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.ActivitiesSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.ActivityGradesSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.AttendanceSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.ClassesSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.GroupsSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.SchedulesSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.StudentsSheetExporter;
import com.edusistem.core.exports.application.schoolsetup.sheets.SubjectsSheetExporter;
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
import java.util.List;
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

    // ------------------------------------------------------------------ export de configuración escolar

    @Bean
    SchoolSetupSnapshotLoader schoolSetupSnapshotLoader(GroupRepositoryPort groups,
                                                        TeachingPeriodRepositoryPort teachingPeriods,
                                                        StudentRepositoryPort students, GradingScaleRepositoryPort scales,
                                                        ActivityRepositoryPort activities) {
        return new DefaultSchoolSetupSnapshotLoader(groups, teachingPeriods, students, scales, activities);
    }

    @Bean
    SchoolSetupSheetExporter academicPeriodsSheetExporter(AcademicPeriodRepositoryPort academicPeriods) {
        return new AcademicPeriodsSheetExporter(academicPeriods);
    }

    @Bean
    SchoolSetupSheetExporter academicGradesSheetExporter(GradeRepositoryPort grades) {
        return new AcademicGradesSheetExporter(grades);
    }

    @Bean
    SchoolSetupSheetExporter subjectsSheetExporter(SubjectRepositoryPort subjects) {
        return new SubjectsSheetExporter(subjects);
    }

    @Bean
    SchoolSetupSheetExporter groupsSheetExporter() {
        return new GroupsSheetExporter();
    }

    @Bean
    SchoolSetupSheetExporter classesSheetExporter(GradingConfigurationRepositoryPort configurations,
                                                  EvaluationCategoryRepositoryPort categories) {
        return new ClassesSheetExporter(configurations, categories);
    }

    @Bean
    SchoolSetupSheetExporter schedulesSheetExporter(TeachingPeriodScheduleRepositoryPort schedules) {
        return new SchedulesSheetExporter(schedules);
    }

    @Bean
    SchoolSetupSheetExporter studentsSheetExporter() {
        return new StudentsSheetExporter();
    }

    @Bean
    SchoolSetupSheetExporter activitiesSheetExporter() {
        return new ActivitiesSheetExporter();
    }

    @Bean
    SchoolSetupSheetExporter activityGradesSheetExporter(GradeActivityUseCase gradeActivity) {
        return new ActivityGradesSheetExporter(gradeActivity);
    }

    @Bean
    SchoolSetupSheetExporter attendanceSheetExporter(AttendanceSessionRepositoryPort sessions,
                                                     AttendanceRecordRepositoryPort records) {
        return new AttendanceSheetExporter(sessions, records);
    }

    /** Recibe los 10 exportadores de hoja; el servicio los ordena según {@code SchoolSetupSheets}. */
    @Bean
    SchoolSetupExportService schoolSetupExportService(SchoolSetupSnapshotLoader loader,
                                                      List<SchoolSetupSheetExporter> exporters,
                                                      SpreadsheetWriterPort writer, RecordAuditUseCase audit) {
        return new SchoolSetupExportService(loader, exporters, writer, audit);
    }
}
