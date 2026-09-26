package com.edusistem.core.imports.infrastructure.config;

import com.edusistem.core.academic.domain.inputports.ManageAcademicPeriodUseCase;
import com.edusistem.core.academic.domain.inputports.ManageGradeUseCase;
import com.edusistem.core.academic.domain.inputports.ManageGroupUseCase;
import com.edusistem.core.academic.domain.inputports.ManageTeachingAssignmentUseCase;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodUseCase;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingAssignmentRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.inputports.ManageActivityUseCase;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.attendance.domain.inputports.ManageAttendanceUseCase;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.imports.application.use_case.service.ImportQueryService;
import com.edusistem.core.imports.application.use_case.service.ImportSchoolSetupService;
import com.edusistem.core.imports.application.use_case.service.ImportStudentsService;
import com.edusistem.core.imports.application.use_case.service.ImportTeachingPeriodDataService;
import com.edusistem.core.imports.application.use_case.service.StudentImportApplier;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.outputports.OwnershipPort;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.student.domain.inputports.RegisterStudentUseCase;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import com.edusistem.core.subject.domain.inputports.ManageSubjectUseCase;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans los casos de uso de la capa application del módulo imports. */
@Configuration
public class ImportBeanConfig {

    @Bean
    StudentImportApplier studentImportApplier(RegisterStudentUseCase students) {
        return new StudentImportApplier(students);
    }

    @Bean
    ImportQueryService importQueryService(ImportBatchRepositoryPort batches, FileStoragePort storage) {
        return new ImportQueryService(batches, storage);
    }

    @Bean
    ImportSchoolSetupService importSchoolSetupService(SpreadsheetReaderPort reader, SpreadsheetWriterPort writer,
                                                      ImportBatchRepositoryPort batches, FileStoragePort storage,
                                                      RecordAuditUseCase audit, Clock clock,
                                                      AcademicPeriodRepositoryPort academicPeriods,
                                                      ManageAcademicPeriodUseCase manageAcademicPeriod,
                                                      GradeRepositoryPort grades, ManageGradeUseCase manageGrade,
                                                      SubjectRepositoryPort subjects,
                                                      ManageSubjectUseCase manageSubject, GroupRepositoryPort groups,
                                                      ManageGroupUseCase manageGroup,
                                                      TeachingAssignmentRepositoryPort teachingAssignments,
                                                      ManageTeachingAssignmentUseCase manageTeachingAssignment,
                                                      TeachingPeriodRepositoryPort teachingPeriods,
                                                      ManageTeachingPeriodUseCase manageTeachingPeriod,
                                                      StudentRepositoryPort students, OwnershipPort ownership,
                                                      StudentImportApplier applier,
                                                      ActivityRepositoryPort activityRepo,
                                                      ManageActivityUseCase manageActivity,
                                                      GradeActivityUseCase gradeActivity,
                                                      AttendanceSessionRepositoryPort sessions,
                                                      ManageAttendanceUseCase attendance) {
        return new ImportSchoolSetupService(reader, writer, batches, storage, audit, clock, academicPeriods,
                                            manageAcademicPeriod, grades, manageGrade, subjects, manageSubject,
                                            groups, manageGroup, teachingAssignments, manageTeachingAssignment,
                                            teachingPeriods, manageTeachingPeriod, students, ownership, applier,
                                            activityRepo, manageActivity, gradeActivity, sessions, attendance);
    }

    @Bean
    ImportStudentsService importStudentsService(SpreadsheetReaderPort reader, SpreadsheetWriterPort writer,
                                                ImportBatchRepositoryPort batches, StudentRepositoryPort students,
                                                GradeRepositoryPort grades, GroupRepositoryPort groups,
                                                OwnershipPort ownership, FileStoragePort storage,
                                                StudentImportApplier applier, RecordAuditUseCase audit, Clock clock) {
        return new ImportStudentsService(reader, writer, batches, students, grades, groups, ownership, storage,
                                         applier, audit, clock);
    }

    @Bean
    ImportTeachingPeriodDataService importTeachingPeriodDataService(SpreadsheetReaderPort reader,
                                                                    SpreadsheetWriterPort writer,
                                                                    ImportBatchRepositoryPort batches,
                                                                    StudentRepositoryPort students,
                                                                    OwnershipPort ownership, OwnershipGuard guard,
                                                                    TeachingPeriodRepositoryPort teachingPeriods,
                                                                    ActivityRepositoryPort activities,
                                                                    GradeActivityUseCase gradeActivity,
                                                                    AttendanceSessionRepositoryPort sessions,
                                                                    ManageAttendanceUseCase attendance,
                                                                    StudentImportApplier applier,
                                                                    FileStoragePort storage,
                                                                    RecordAuditUseCase audit, Clock clock) {
        return new ImportTeachingPeriodDataService(reader, writer, batches, students, ownership, guard,
                                                   teachingPeriods, activities, gradeActivity, sessions, attendance,
                                                   applier, storage, audit, clock);
    }
}
