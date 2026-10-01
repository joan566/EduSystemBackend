package com.edusistem.core.imports.infrastructure.config;

import com.edusistem.core.academic.domain.inputports.ManageAcademicPeriodUseCase;
import com.edusistem.core.academic.domain.inputports.ManageGradeUseCase;
import com.edusistem.core.academic.domain.inputports.ManageGroupUseCase;
import com.edusistem.core.academic.domain.inputports.ManageTeachingAssignmentUseCase;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodScheduleUseCase;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodUseCase;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingAssignmentRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.inputports.ManageActivityUseCase;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.attendance.domain.inputports.ManageAttendanceUseCase;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.grading.domain.inputports.ConfigureGradingUseCase;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.imports.application.batch.DefaultImportBatchTracker;
import com.edusistem.core.imports.application.classsetup.GradingSetupApplier;
import com.edusistem.core.imports.application.classsetup.ScheduleSetupApplier;
import com.edusistem.core.imports.application.contracts.ClassGradingConfigurer;
import com.edusistem.core.imports.application.contracts.ClassResolver;
import com.edusistem.core.imports.application.contracts.ClassScheduleConfigurer;
import com.edusistem.core.imports.application.contracts.ImportBatchTracker;
import com.edusistem.core.imports.application.contracts.SchoolSetupSheetImporter;
import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.imports.application.contracts.TeachingPeriodCatalog;
import com.edusistem.core.imports.application.schoolsetup.resolution.DefaultClassResolver;
import com.edusistem.core.imports.application.schoolsetup.resolution.DefaultStudentRoster;
import com.edusistem.core.imports.application.schoolsetup.resolution.DefaultTeachingPeriodCatalog;
import com.edusistem.core.imports.application.schoolsetup.sheets.AcademicGradesSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.AcademicPeriodsSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.ActivitiesSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.ActivityGradesSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.AttendanceSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.ClassesSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.GroupsSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.SchedulesSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.StudentsSheetImporter;
import com.edusistem.core.imports.application.schoolsetup.sheets.SubjectsSheetImporter;
import com.edusistem.core.imports.application.student.StudentImportApplier;
import com.edusistem.core.imports.application.teachingperiod.sheets.PeriodAttendanceSheetImporter;
import com.edusistem.core.imports.application.teachingperiod.sheets.PeriodGradesSheetImporter;
import com.edusistem.core.imports.application.teachingperiod.sheets.PeriodStudentsSheetImporter;
import com.edusistem.core.imports.application.use_case.service.ImportFileRetentionService;
import com.edusistem.core.imports.application.use_case.service.ImportQueryService;
import com.edusistem.core.imports.application.use_case.service.ImportSchoolSetupService;
import com.edusistem.core.imports.application.use_case.service.ImportStudentsService;
import com.edusistem.core.imports.application.use_case.service.ImportTeachingPeriodDataService;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.student.domain.inputports.RegisterStudentUseCase;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import com.edusistem.core.subject.domain.inputports.ManageSubjectUseCase;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo imports y sus colaboradores. */
@Configuration
public class ImportBeanConfig {

    // ------------------------------------------------------------------ colaboradores compartidos

    @Bean
    StudentImportApplier studentImportApplier(RegisterStudentUseCase students) {
        return new StudentImportApplier(students);
    }

    @Bean
    ImportBatchTracker importBatchTracker(ImportBatchRepositoryPort batches, FileStoragePort storage,
                                          SpreadsheetWriterPort writer, RecordAuditUseCase audit, Clock clock) {
        return new DefaultImportBatchTracker(batches, storage, writer, audit, clock);
    }

    @Bean
    ClassResolver importClassResolver(GroupRepositoryPort groups, SubjectRepositoryPort subjects,
                                      AcademicPeriodRepositoryPort academicPeriods,
                                      TeachingAssignmentRepositoryPort teachingAssignments,
                                      ManageTeachingAssignmentUseCase manageTeachingAssignment,
                                      TeachingPeriodRepositoryPort teachingPeriods,
                                      ManageTeachingPeriodUseCase manageTeachingPeriod) {
        return new DefaultClassResolver(groups, subjects, academicPeriods, teachingAssignments,
                                        manageTeachingAssignment, teachingPeriods, manageTeachingPeriod);
    }

    @Bean
    StudentRoster importStudentRoster(StudentRepositoryPort students, TeachingPeriodRepositoryPort teachingPeriods) {
        return new DefaultStudentRoster(students, teachingPeriods);
    }

    @Bean
    TeachingPeriodCatalog importTeachingPeriodCatalog(ActivityRepositoryPort activities,
                                                      AttendanceSessionRepositoryPort sessions) {
        return new DefaultTeachingPeriodCatalog(activities, sessions);
    }

    @Bean
    ClassGradingConfigurer classGradingConfigurer(ConfigureGradingUseCase configureGrading,
                                                  GradingConfigurationRepositoryPort configurations,
                                                  GradingScaleRepositoryPort scales,
                                                  EvaluationCategoryRepositoryPort categories) {
        return new GradingSetupApplier(configureGrading, configurations, scales, categories);
    }

    @Bean
    ClassScheduleConfigurer classScheduleConfigurer(ManageTeachingPeriodScheduleUseCase manageSchedule,
                                                    TeachingPeriodScheduleRepositoryPort schedules,
                                                    TeachingPeriodRepositoryPort teachingPeriods) {
        return new ScheduleSetupApplier(manageSchedule, schedules, teachingPeriods);
    }

    // ------------------------------------------------------------------ hojas de configuración escolar

    @Bean
    SchoolSetupSheetImporter academicPeriodsSheetImporter(AcademicPeriodRepositoryPort academicPeriods,
                                                          ManageAcademicPeriodUseCase manageAcademicPeriod) {
        return new AcademicPeriodsSheetImporter(academicPeriods, manageAcademicPeriod);
    }

    @Bean
    SchoolSetupSheetImporter academicGradesSheetImporter(GradeRepositoryPort grades, ManageGradeUseCase manageGrade) {
        return new AcademicGradesSheetImporter(grades, manageGrade);
    }

    @Bean
    SchoolSetupSheetImporter subjectsSheetImporter(SubjectRepositoryPort subjects, ManageSubjectUseCase manageSubject) {
        return new SubjectsSheetImporter(subjects, manageSubject);
    }

    @Bean
    SchoolSetupSheetImporter groupsSheetImporter(GroupRepositoryPort groups, GradeRepositoryPort grades,
                                                 ManageGroupUseCase manageGroup) {
        return new GroupsSheetImporter(groups, grades, manageGroup);
    }

    @Bean
    SchoolSetupSheetImporter classesSheetImporter(GroupRepositoryPort groups, SubjectRepositoryPort subjects,
                                                  AcademicPeriodRepositoryPort academicPeriods, ClassResolver classes,
                                                  ClassGradingConfigurer grading) {
        return new ClassesSheetImporter(groups, subjects, academicPeriods, classes, grading);
    }

    @Bean
    SchoolSetupSheetImporter schedulesSheetImporter(ClassResolver classes, ClassScheduleConfigurer schedules) {
        return new SchedulesSheetImporter(classes, schedules);
    }

    @Bean
    SchoolSetupSheetImporter studentsSheetImporter(StudentRepositoryPort students, GroupRepositoryPort groups,
                                                   StudentImportApplier applier, Clock clock) {
        return new StudentsSheetImporter(students, groups, applier, clock);
    }

    @Bean
    SchoolSetupSheetImporter activitiesSheetImporter(ClassResolver classes, TeachingPeriodCatalog catalog,
                                                     ManageActivityUseCase manageActivity) {
        return new ActivitiesSheetImporter(classes, catalog, manageActivity);
    }

    @Bean
    SchoolSetupSheetImporter activityGradesSheetImporter(ClassResolver classes, TeachingPeriodCatalog catalog,
                                                         StudentRoster roster, GradeActivityUseCase gradeActivity) {
        return new ActivityGradesSheetImporter(classes, catalog, roster, gradeActivity);
    }

    @Bean
    SchoolSetupSheetImporter attendanceSheetImporter(ClassResolver classes, TeachingPeriodCatalog catalog,
                                                     StudentRoster roster, ManageAttendanceUseCase attendance) {
        return new AttendanceSheetImporter(classes, catalog, roster, attendance);
    }

    // ------------------------------------------------------------------ casos de uso

    @Bean
    ImportQueryService importQueryService(ImportBatchRepositoryPort batches, FileStoragePort storage) {
        return new ImportQueryService(batches, storage);
    }

    @Bean
    ImportFileRetentionService importFileRetentionService(ImportBatchRepositoryPort batches, FileStoragePort storage) {
        return new ImportFileRetentionService(batches, storage);
    }

    /** Recibe los 10 importadores de hoja; el servicio los ordena según {@code SchoolSetupSheets}. */
    @Bean
    ImportSchoolSetupService importSchoolSetupService(ImportBatchTracker batches, SpreadsheetReaderPort reader,
                                                      SpreadsheetWriterPort writer, ClassGradingConfigurer grading,
                                                      List<SchoolSetupSheetImporter> importers) {
        return new ImportSchoolSetupService(batches, reader, writer, grading, importers);
    }

    @Bean
    ImportStudentsService importStudentsService(ImportBatchTracker batches, SpreadsheetReaderPort reader,
                                                SpreadsheetWriterPort writer, StudentRepositoryPort students,
                                                GradeRepositoryPort grades, GroupRepositoryPort groups,
                                                StudentImportApplier applier, Clock clock) {
        return new ImportStudentsService(batches, reader, writer, students, grades, groups, applier, clock);
    }

    /** Las hojas del teaching period se procesan en este orden: Estudiantes primero. */
    @Bean
    ImportTeachingPeriodDataService importTeachingPeriodDataService(ImportBatchTracker batches,
                                                                    SpreadsheetReaderPort reader, OwnershipGuard guard,
                                                                    TeachingPeriodRepositoryPort teachingPeriods,
                                                                    StudentRoster roster,
                                                                    StudentRepositoryPort students,
                                                                    StudentImportApplier applier,
                                                                    TeachingPeriodCatalog catalog,
                                                                    GradeActivityUseCase gradeActivity,
                                                                    ManageAttendanceUseCase attendance) {
        return new ImportTeachingPeriodDataService(batches, reader, guard, teachingPeriods, roster, List.of(
                new PeriodStudentsSheetImporter(students, applier),
                new PeriodGradesSheetImporter(catalog, gradeActivity),
                new PeriodAttendanceSheetImporter(catalog, attendance)));
    }
}
