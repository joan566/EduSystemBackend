package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.entity.Grade;
import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.entity.TeachingAssignment;
import com.edusistem.core.academic.domain.entity.TeachingPeriod;
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
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.inputports.ManageActivityUseCase;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.attendance.application.use_case.dtos.AttendanceCommands;
import com.edusistem.core.attendance.domain.enums.AttendanceStatus;
import com.edusistem.core.attendance.domain.inputports.ManageAttendanceUseCase;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.domain.inputports.ImportSchoolSetupUseCase;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.imports.domain.vo.ImportResult;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.ParsedWorkbook;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.outputports.OwnershipPort;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import com.edusistem.core.subject.application.use_case.dtos.SubjectCommands;
import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.domain.inputports.ManageSubjectUseCase;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Importación combinada de la configuración completa de un profesor: 9 hojas, todas opcionales, procesadas en orden
 * de dependencia (AcademicPeriods, AcademicGrades, Subjects, Groups, Classes, Students, Activities, ActivityGrades,
 * Attendance). A diferencia de {@link ImportTeachingPeriodDataService} (que edita un teaching period que ya existe,
 * con columnas dinámicas por id), aquí los ids todavía no existen: cada hoja referencia sus dependencias por nombre
 * y se resuelven ("buscar o crear") contra la base de datos a medida que se procesan las hojas en orden. Cada fila
 * se valida antes de aplicarse: una fila o celda incorrecta no aborta el resto de la importación.
 */
@Service
public class ImportSchoolSetupService implements ImportSchoolSetupUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportSchoolSetupService.class);
    private static final int MAX_RETURNED_ERRORS = 500;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SpreadsheetReaderPort reader;
    private final SpreadsheetWriterPort writer;
    private final ImportBatchRepositoryPort batches;
    private final FileStoragePort storage;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    private final AcademicPeriodRepositoryPort academicPeriods;
    private final ManageAcademicPeriodUseCase manageAcademicPeriod;
    private final GradeRepositoryPort grades;
    private final ManageGradeUseCase manageGrade;
    private final SubjectRepositoryPort subjects;
    private final ManageSubjectUseCase manageSubject;
    private final GroupRepositoryPort groups;
    private final ManageGroupUseCase manageGroup;
    private final TeachingAssignmentRepositoryPort teachingAssignments;
    private final ManageTeachingAssignmentUseCase manageTeachingAssignment;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final ManageTeachingPeriodUseCase manageTeachingPeriod;
    private final StudentRepositoryPort students;
    private final OwnershipPort ownership;
    private final StudentImportApplier applier;
    private final ActivityRepositoryPort activityRepo;
    private final ManageActivityUseCase manageActivity;
    private final GradeActivityUseCase gradeActivity;
    private final AttendanceSessionRepositoryPort sessions;
    private final ManageAttendanceUseCase attendance;

    public ImportSchoolSetupService(SpreadsheetReaderPort reader, SpreadsheetWriterPort writer,
                                    ImportBatchRepositoryPort batches, FileStoragePort storage, RecordAuditUseCase audit,
                                    Clock clock, AcademicPeriodRepositoryPort academicPeriods,
                                    ManageAcademicPeriodUseCase manageAcademicPeriod, GradeRepositoryPort grades,
                                    ManageGradeUseCase manageGrade, SubjectRepositoryPort subjects,
                                    ManageSubjectUseCase manageSubject, GroupRepositoryPort groups,
                                    ManageGroupUseCase manageGroup, TeachingAssignmentRepositoryPort teachingAssignments,
                                    ManageTeachingAssignmentUseCase manageTeachingAssignment,
                                    TeachingPeriodRepositoryPort teachingPeriods,
                                    ManageTeachingPeriodUseCase manageTeachingPeriod, StudentRepositoryPort students,
                                    OwnershipPort ownership, StudentImportApplier applier, ActivityRepositoryPort activityRepo,
                                    ManageActivityUseCase manageActivity, GradeActivityUseCase gradeActivity,
                                    AttendanceSessionRepositoryPort sessions, ManageAttendanceUseCase attendance) {
        this.reader = reader;
        this.writer = writer;
        this.batches = batches;
        this.storage = storage;
        this.audit = audit;
        this.clock = clock;
        this.academicPeriods = academicPeriods;
        this.manageAcademicPeriod = manageAcademicPeriod;
        this.grades = grades;
        this.manageGrade = manageGrade;
        this.subjects = subjects;
        this.manageSubject = manageSubject;
        this.groups = groups;
        this.manageGroup = manageGroup;
        this.teachingAssignments = teachingAssignments;
        this.manageTeachingAssignment = manageTeachingAssignment;
        this.teachingPeriods = teachingPeriods;
        this.manageTeachingPeriod = manageTeachingPeriod;
        this.students = students;
        this.ownership = ownership;
        this.applier = applier;
        this.activityRepo = activityRepo;
        this.manageActivity = manageActivity;
        this.gradeActivity = gradeActivity;
        this.sessions = sessions;
        this.attendance = attendance;
    }

    private record ClassKey(String gradeName, String groupName, int academicYear, String subjectName,
                            String academicPeriodName) {
    }

    private record ActivityRef(Long activityId, BigDecimal maximumScore) {
    }

    @Override
    public ImportResult importData(ImportCommands.ImportSchoolSetup command) {
        Long teacherId = command.teacherId();
        String fileName = command.fileName() == null ? "school-setup.xlsx" : command.fileName();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx") || !looksLikeZip(command.content())) {
            throw new InvalidRequestException("INVALID_FILE_TYPE", "Only Excel .xlsx files are supported");
        }
        ImportBatch batch = batches.save(ImportBatch.builder().userId(teacherId)
                .fileName(fileName.length() > 255 ? fileName.substring(0, 255) : fileName)
                .filePath(storage.store("imports", fileName, command.content()))
                .status(ImportStatus.PROCESSING).build());
        try {
            ParsedWorkbook workbook = reader.readAll(command.content());
            List<ImportRowError> errors = new ArrayList<>();
            Set<String> failedKeys = new HashSet<>();

            Map<ClassKey, Long> teachingPeriodCache = new HashMap<>();
            Map<Long, Map<String, ActivityRef>> activityByPeriodThenName = new HashMap<>();
            Map<Long, Map<String, Long>> studentByPeriodThenIdentification = new HashMap<>();
            Map<Long, Map<LocalDate, Long>> sessionByPeriodThenDate = new HashMap<>();

            int totalRows = applyAcademicPeriods(teacherId, workbook, errors, failedKeys);
            totalRows += applyAcademicGrades(teacherId, workbook, errors, failedKeys);
            totalRows += applySubjects(teacherId, workbook, errors, failedKeys);
            totalRows += applyGroups(teacherId, workbook, errors, failedKeys);
            totalRows += applyClasses(teacherId, workbook, teachingPeriodCache, errors, failedKeys);
            totalRows += applyStudents(teacherId, workbook, errors, failedKeys);
            totalRows += applyActivities(teacherId, workbook, teachingPeriodCache, activityByPeriodThenName, errors, failedKeys);
            totalRows += applyActivityGrades(teacherId, workbook, teachingPeriodCache, activityByPeriodThenName,
                    studentByPeriodThenIdentification, errors, failedKeys);
            totalRows += applyAttendance(teacherId, workbook, teachingPeriodCache, sessionByPeriodThenDate,
                    studentByPeriodThenIdentification, errors, failedKeys);

            return finish(batch, teacherId, totalRows, totalRows - failedKeys.size(), errors, failedKeys.size());
        } catch (RuntimeException e) {
            fail(batch, teacherId, e);
            throw e;
        }
    }

    @Override
    public byte[] template() {
        return writer.writeWorkbook(List.of(
                new TabularData("AcademicPeriods", List.of("name", "start_date", "end_date"),
                        List.of(List.of("2026-1", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 15)))),
                new TabularData("AcademicGrades", List.of("name", "description"),
                        List.of(List.of("10°", "Décimo grado"))),
                new TabularData("Subjects", List.of("name", "description"),
                        List.of(List.of("Matemáticas", "Matemáticas de grado 10"))),
                new TabularData("Groups", List.of("grade_name", "name", "academic_year"),
                        List.of(List.of("10°", "A", 2026))),
                new TabularData("Classes",
                        List.of("grade_name", "group_name", "academic_year", "subject_name", "academic_period_name"),
                        List.of(List.of("10°", "A", 2026, "Matemáticas", "2026-1"))),
                new TabularData("Students",
                        List.of("identification_number", "first_name", "last_name", "email", "grade_name", "group_name",
                                "academic_year"),
                        List.of(List.of("1001234567", "Ana", "Pérez", "ana.perez@example.com", "10°", "A", 2026))),
                new TabularData("Activities",
                        List.of("grade_name", "group_name", "academic_year", "subject_name", "academic_period_name",
                                "name", "description", "evaluation_date", "maximum_score", "activity_type"),
                        List.of(List.of("10°", "A", 2026, "Matemáticas", "2026-1", "Taller 1", "Taller de repaso",
                                LocalDateTime.of(2026, 2, 10, 9, 0), BigDecimal.valueOf(5), "TALLER"))),
                new TabularData("ActivityGrades",
                        List.of("grade_name", "group_name", "academic_year", "subject_name", "academic_period_name",
                                "activity_name", "identification_number", "grade"),
                        List.of(List.of("10°", "A", 2026, "Matemáticas", "2026-1", "Taller 1", "1001234567",
                                BigDecimal.valueOf(4.5)))),
                new TabularData("Attendance",
                        List.of("grade_name", "group_name", "academic_year", "subject_name", "academic_period_name",
                                "session_date", "identification_number", "status"),
                        List.of(List.of("10°", "A", 2026, "Matemáticas", "2026-1", LocalDate.of(2026, 2, 10),
                                "1001234567", "PRESENT")))));
    }

    // ------------------------------------------------------------------ AcademicPeriods

    private int applyAcademicPeriods(Long teacherId, ParsedWorkbook wb, List<ImportRowError> errors, Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("AcademicPeriods");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "name", "AcademicPeriods");
        requireColumn(sheet, "start_date", "AcademicPeriods");
        requireColumn(sheet, "end_date", "AcademicPeriods");

        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            String name = required(row, "name", 100, errors, "AcademicPeriods");
            LocalDate startDate = parseDate(row, "start_date", "AcademicPeriods", errors);
            LocalDate endDate = parseDate(row, "end_date", "AcademicPeriods", errors);
            if (errors.size() > before) {
                failedKeys.add("AcademicPeriods#" + row.rowNumber());
                continue;
            }
            try {
                if (academicPeriods.findByName(name).isEmpty()) {
                    manageAcademicPeriod.create(new AcademicCommands.SavePeriod(teacherId, null, name, startDate, endDate));
                }
            } catch (RuntimeException e) {
                errors.add(new ImportRowError(row.rowNumber(), "AcademicPeriods", "Could not create: " + e.getMessage()));
                failedKeys.add("AcademicPeriods#" + row.rowNumber());
            }
        }
        return sheet.rows().size();
    }

    // ------------------------------------------------------------------ AcademicGrades

    private int applyAcademicGrades(Long teacherId, ParsedWorkbook wb, List<ImportRowError> errors, Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("AcademicGrades");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "name", "AcademicGrades");

        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            String name = required(row, "name", 50, errors, "AcademicGrades");
            String description = optional(row, "description", 255, errors, "AcademicGrades");
            if (errors.size() > before) {
                failedKeys.add("AcademicGrades#" + row.rowNumber());
                continue;
            }
            try {
                if (grades.findByName(name).isEmpty()) {
                    manageGrade.create(new AcademicCommands.CreateGrade(teacherId, name, description));
                }
            } catch (RuntimeException e) {
                errors.add(new ImportRowError(row.rowNumber(), "AcademicGrades", "Could not create: " + e.getMessage()));
                failedKeys.add("AcademicGrades#" + row.rowNumber());
            }
        }
        return sheet.rows().size();
    }

    // ------------------------------------------------------------------ Subjects

    private int applySubjects(Long teacherId, ParsedWorkbook wb, List<ImportRowError> errors, Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("Subjects");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "name", "Subjects");

        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            String name = required(row, "name", 100, errors, "Subjects");
            String description = optional(row, "description", 255, errors, "Subjects");
            if (errors.size() > before) {
                failedKeys.add("Subjects#" + row.rowNumber());
                continue;
            }
            try {
                if (subjects.findByName(name).isEmpty()) {
                    manageSubject.create(new SubjectCommands.Create(teacherId, name, description));
                }
            } catch (RuntimeException e) {
                errors.add(new ImportRowError(row.rowNumber(), "Subjects", "Could not create: " + e.getMessage()));
                failedKeys.add("Subjects#" + row.rowNumber());
            }
        }
        return sheet.rows().size();
    }

    // ------------------------------------------------------------------ Groups

    private int applyGroups(Long teacherId, ParsedWorkbook wb, List<ImportRowError> errors, Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("Groups");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "grade_name", "Groups");
        requireColumn(sheet, "name", "Groups");
        requireColumn(sheet, "academic_year", "Groups");

        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            String gradeName = required(row, "grade_name", 50, errors, "Groups");
            String name = required(row, "name", 50, errors, "Groups");
            Integer year = requiredYear(row, "academic_year", "Groups", errors);
            if (errors.size() > before) {
                failedKeys.add("Groups#" + row.rowNumber());
                continue;
            }
            try {
                if (groups.findByGradeNameAndNameAndAcademicYear(gradeName, name, year).isEmpty()) {
                    Long gradeId = grades.findByName(gradeName).map(Grade::getId).orElse(null);
                    if (gradeId == null) {
                        errors.add(new ImportRowError(row.rowNumber(), "Groups:grade_name",
                                "Grade '" + gradeName + "' does not exist (add it to the AcademicGrades sheet)"));
                        failedKeys.add("Groups#" + row.rowNumber());
                        continue;
                    }
                    manageGroup.create(new AcademicCommands.CreateGroup(teacherId, gradeId, name, year));
                }
            } catch (RuntimeException e) {
                errors.add(new ImportRowError(row.rowNumber(), "Groups", "Could not create: " + e.getMessage()));
                failedKeys.add("Groups#" + row.rowNumber());
            }
        }
        return sheet.rows().size();
    }

    // ------------------------------------------------------------------ Classes

    private int applyClasses(Long teacherId, ParsedWorkbook wb, Map<ClassKey, Long> teachingPeriodCache,
                             List<ImportRowError> errors, Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("Classes");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "grade_name", "Classes");
        requireColumn(sheet, "group_name", "Classes");
        requireColumn(sheet, "academic_year", "Classes");
        requireColumn(sheet, "subject_name", "Classes");
        requireColumn(sheet, "academic_period_name", "Classes");

        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            ClassKey key = requiredClassKey(row, "Classes", errors);
            if (errors.size() > before || key == null) {
                failedKeys.add("Classes#" + row.rowNumber());
                continue;
            }
            try {
                Optional<Group> group = groups.findByGradeNameAndNameAndAcademicYear(key.gradeName(), key.groupName(),
                        key.academicYear());
                if (group.isEmpty()) {
                    errors.add(new ImportRowError(row.rowNumber(), "Classes:group_name",
                            "Group '" + key.groupName() + "' does not exist in grade '" + key.gradeName() + "' for "
                                    + key.academicYear() + " (add it to the Groups sheet)"));
                    failedKeys.add("Classes#" + row.rowNumber());
                    continue;
                }
                Optional<Subject> subject = subjects.findByName(key.subjectName());
                if (subject.isEmpty()) {
                    errors.add(new ImportRowError(row.rowNumber(), "Classes:subject_name",
                            "Subject '" + key.subjectName() + "' does not exist (add it to the Subjects sheet)"));
                    failedKeys.add("Classes#" + row.rowNumber());
                    continue;
                }
                Optional<AcademicPeriod> period = academicPeriods.findByName(key.academicPeriodName());
                if (period.isEmpty()) {
                    errors.add(new ImportRowError(row.rowNumber(), "Classes:academic_period_name",
                            "Academic period '" + key.academicPeriodName()
                                    + "' does not exist (add it to the AcademicPeriods sheet)"));
                    failedKeys.add("Classes#" + row.rowNumber());
                    continue;
                }
                Long groupId = group.get().getId();
                Long subjectId = subject.get().getId();
                Long assignmentId = teachingAssignments.findByTeacherIdAndGroupIdAndSubjectId(teacherId, groupId, subjectId)
                        .map(TeachingAssignment::getId)
                        .orElseGet(() -> manageTeachingAssignment
                                .create(new AcademicCommands.CreateTeachingAssignment(teacherId, groupId, subjectId)).id());
                Long periodId = period.get().getId();
                Long teachingPeriodId = teachingPeriods.findByTeachingAssignmentIdAndAcademicPeriodId(assignmentId, periodId)
                        .map(TeachingPeriod::getId)
                        .orElseGet(() -> manageTeachingPeriod
                                .create(new AcademicCommands.CreateTeachingPeriod(teacherId, assignmentId, periodId)).id());
                teachingPeriodCache.put(key, teachingPeriodId);
            } catch (RuntimeException e) {
                errors.add(new ImportRowError(row.rowNumber(), "Classes", "Could not create: " + e.getMessage()));
                failedKeys.add("Classes#" + row.rowNumber());
            }
        }
        return sheet.rows().size();
    }

    // ------------------------------------------------------------------ Students

    private int applyStudents(Long teacherId, ParsedWorkbook wb, List<ImportRowError> errors, Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("Students");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "identification_number", "Students");
        requireColumn(sheet, "first_name", "Students");
        requireColumn(sheet, "last_name", "Students");
        requireColumn(sheet, "grade_name", "Students");
        requireColumn(sheet, "group_name", "Students");

        List<StudentImportRow> valid = new ArrayList<>();
        Map<String, Integer> seenIdentifications = new HashMap<>();
        Map<String, Optional<Long>> groupCache = new HashMap<>();
        Map<Long, Boolean> teachesCache = new HashMap<>();
        int currentYear = LocalDateTime.now(clock).getYear();

        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            String identification = required(row, "identification_number", 50, errors, "Students");
            String firstName = required(row, "first_name", 100, errors, "Students");
            String lastName = required(row, "last_name", 100, errors, "Students");
            String gradeName = required(row, "grade_name", 50, errors, "Students");
            String groupName = required(row, "group_name", 50, errors, "Students");
            String email = row.get("email");
            if (email != null && (email.length() > 255 || !EMAIL.matcher(email).matches())) {
                errors.add(new ImportRowError(row.rowNumber(), "Students:email", "Invalid e-mail address"));
            }
            int year = currentYear;
            String yearText = row.get("academic_year");
            if (yearText != null) {
                try {
                    year = Integer.parseInt(yearText);
                    if (year < 2000 || year > 2200) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    errors.add(new ImportRowError(row.rowNumber(), "Students:academic_year",
                            "Must be an integer year between 2000 and 2200"));
                }
            }
            if (identification != null) {
                Integer first = seenIdentifications.putIfAbsent(identification, row.rowNumber());
                if (first != null) {
                    errors.add(new ImportRowError(row.rowNumber(), "Students:identification_number",
                            "Duplicated in the file (first seen in row " + first + ")"));
                }
            }
            Long groupId = null;
            if (gradeName != null && groupName != null && errors.size() == before) {
                groupId = resolveEnrollmentGroup(row, gradeName, groupName, year, groupCache, teachesCache, teacherId, errors);
            }
            if (errors.size() > before) {
                failedKeys.add("Students#" + row.rowNumber());
                continue;
            }
            Long existingId = students.findByIdentificationNumber(identification).map(Student::getId).orElse(null);
            boolean update = existingId != null && ownership.teachesStudent(teacherId, existingId);
            valid.add(new StudentImportRow(row.rowNumber(), existingId, update, identification, null, firstName, lastName,
                    email, groupId));
        }
        applier.apply(valid);
        return sheet.rows().size();
    }

    private Long resolveEnrollmentGroup(SpreadsheetRow row, String gradeName, String groupName, int year,
                                        Map<String, Optional<Long>> cache, Map<Long, Boolean> teachesCache, Long teacherId,
                                        List<ImportRowError> errors) {
        String key = gradeName + "|" + groupName + "|" + year;
        Optional<Long> groupId = cache.computeIfAbsent(key,
                k -> groups.findByGradeNameAndNameAndAcademicYear(gradeName, groupName, year).map(Group::getId));
        if (groupId.isEmpty()) {
            errors.add(new ImportRowError(row.rowNumber(), "Students:group_name",
                    "Group '" + groupName + "' does not exist in grade '" + gradeName + "' for " + year
                            + " (add it to the Groups sheet)"));
            return null;
        }
        boolean teaches = teachesCache.computeIfAbsent(groupId.get(), id -> ownership.teachesGroup(teacherId, id));
        if (!teaches) {
            errors.add(new ImportRowError(row.rowNumber(), "Students:group_name",
                    "You have no class registered for group " + gradeName + " " + groupName + " (" + year + ")"));
            return null;
        }
        return groupId.get();
    }

    // ------------------------------------------------------------------ Activities

    private int applyActivities(Long teacherId, ParsedWorkbook wb, Map<ClassKey, Long> teachingPeriodCache,
                                Map<Long, Map<String, ActivityRef>> activityByPeriodThenName, List<ImportRowError> errors,
                                Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("Activities");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "grade_name", "Activities");
        requireColumn(sheet, "group_name", "Activities");
        requireColumn(sheet, "academic_year", "Activities");
        requireColumn(sheet, "subject_name", "Activities");
        requireColumn(sheet, "academic_period_name", "Activities");
        requireColumn(sheet, "name", "Activities");
        requireColumn(sheet, "maximum_score", "Activities");

        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            ClassKey key = requiredClassKey(row, "Activities", errors);
            String name = required(row, "name", 150, errors, "Activities");
            String activityType = optional(row, "activity_type", 50, errors, "Activities");
            BigDecimal maximumScore = requiredPositiveDecimal(row, "maximum_score", "Activities", errors);
            LocalDateTime evaluationDate = parseDateTimeOrNull(row, "evaluation_date", "Activities", errors);
            String description = row.get("description");
            if (errors.size() > before || key == null) {
                failedKeys.add("Activities#" + row.rowNumber());
                continue;
            }
            Long teachingPeriodId = resolveTeachingPeriod(row, "Activities", teacherId, key, teachingPeriodCache, errors);
            if (teachingPeriodId == null) {
                failedKeys.add("Activities#" + row.rowNumber());
                continue;
            }
            Map<String, ActivityRef> existing = activityByPeriodThenName.computeIfAbsent(teachingPeriodId,
                    this::loadExistingActivities);
            if (existing.containsKey(name)) {
                continue;
            }
            try {
                ActivityView created = manageActivity.create(new ActivityCommands.Create(teacherId, teachingPeriodId, name,
                        description, evaluationDate, maximumScore, activityType));
                existing.put(name, new ActivityRef(created.activityId(), created.maximumScore()));
            } catch (RuntimeException e) {
                errors.add(new ImportRowError(row.rowNumber(), "Activities", "Could not create: " + e.getMessage()));
                failedKeys.add("Activities#" + row.rowNumber());
            }
        }
        return sheet.rows().size();
    }

    private Map<String, ActivityRef> loadExistingActivities(Long teachingPeriodId) {
        Map<String, ActivityRef> map = new HashMap<>();
        PageResult<ActivityView> page;
        int n = 0;
        do {
            page = activityRepo.findViewsByTeachingPeriodId(teachingPeriodId, new PageQuery(n++, PageQuery.MAX_SIZE));
            page.items().forEach(a -> map.put(a.name(), new ActivityRef(a.activityId(), a.maximumScore())));
        } while (n < page.totalPages());
        return map;
    }

    // ------------------------------------------------------------------ ActivityGrades

    private int applyActivityGrades(Long teacherId, ParsedWorkbook wb, Map<ClassKey, Long> teachingPeriodCache,
                                    Map<Long, Map<String, ActivityRef>> activityByPeriodThenName,
                                    Map<Long, Map<String, Long>> studentByPeriodThenIdentification,
                                    List<ImportRowError> errors, Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("ActivityGrades");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "grade_name", "ActivityGrades");
        requireColumn(sheet, "group_name", "ActivityGrades");
        requireColumn(sheet, "academic_year", "ActivityGrades");
        requireColumn(sheet, "subject_name", "ActivityGrades");
        requireColumn(sheet, "academic_period_name", "ActivityGrades");
        requireColumn(sheet, "activity_name", "ActivityGrades");
        requireColumn(sheet, "identification_number", "ActivityGrades");
        requireColumn(sheet, "grade", "ActivityGrades");

        Map<Long, List<ActivityCommands.GradeInput>> inputsByActivity = new HashMap<>();
        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            ClassKey key = requiredClassKey(row, "ActivityGrades", errors);
            String activityName = required(row, "activity_name", 150, errors, "ActivityGrades");
            String identification = required(row, "identification_number", 50, errors, "ActivityGrades");
            String rawGrade = required(row, "grade", 20, errors, "ActivityGrades");
            if (errors.size() > before || key == null) {
                failedKeys.add("ActivityGrades#" + row.rowNumber());
                continue;
            }
            Long teachingPeriodId = resolveTeachingPeriod(row, "ActivityGrades", teacherId, key, teachingPeriodCache, errors);
            if (teachingPeriodId == null) {
                failedKeys.add("ActivityGrades#" + row.rowNumber());
                continue;
            }
            ActivityRef activity = activityByPeriodThenName.computeIfAbsent(teachingPeriodId, this::loadExistingActivities)
                    .get(activityName);
            if (activity == null) {
                errors.add(new ImportRowError(row.rowNumber(), "ActivityGrades:activity_name",
                        "Activity '" + activityName + "' not found for that class (add it to the Activities sheet)"));
                failedKeys.add("ActivityGrades#" + row.rowNumber());
                continue;
            }
            Long studentId = resolveStudentInPeriod(row, "ActivityGrades", teachingPeriodId, identification,
                    studentByPeriodThenIdentification, errors);
            if (studentId == null) {
                failedKeys.add("ActivityGrades#" + row.rowNumber());
                continue;
            }
            BigDecimal grade;
            try {
                grade = new BigDecimal(rawGrade);
            } catch (NumberFormatException e) {
                errors.add(new ImportRowError(row.rowNumber(), "ActivityGrades:grade", "Must be a number"));
                failedKeys.add("ActivityGrades#" + row.rowNumber());
                continue;
            }
            if (grade.signum() < 0 || grade.compareTo(activity.maximumScore()) > 0) {
                errors.add(new ImportRowError(row.rowNumber(), "ActivityGrades:grade",
                        "Must be between 0 and " + activity.maximumScore().stripTrailingZeros().toPlainString()));
                failedKeys.add("ActivityGrades#" + row.rowNumber());
                continue;
            }
            inputsByActivity.computeIfAbsent(activity.activityId(), k -> new ArrayList<>())
                    .add(new ActivityCommands.GradeInput(studentId, grade, null));
        }
        inputsByActivity.forEach((activityId, inputs) -> {
            try {
                gradeActivity.recordGrades(new ActivityCommands.RecordGrades(teacherId, activityId, inputs));
            } catch (RuntimeException e) {
                log.warn("Could not save grades for activity {}: {}", activityId, e.getMessage());
                errors.add(new ImportRowError(0, "ActivityGrades:activity-" + activityId, "Could not save: " + e.getMessage()));
            }
        });
        return sheet.rows().size();
    }

    // ------------------------------------------------------------------ Attendance

    private int applyAttendance(Long teacherId, ParsedWorkbook wb, Map<ClassKey, Long> teachingPeriodCache,
                                Map<Long, Map<LocalDate, Long>> sessionByPeriodThenDate,
                                Map<Long, Map<String, Long>> studentByPeriodThenIdentification, List<ImportRowError> errors,
                                Set<String> failedKeys) {
        Optional<ParsedSheet> sheetOpt = wb.sheet("Attendance");
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "grade_name", "Attendance");
        requireColumn(sheet, "group_name", "Attendance");
        requireColumn(sheet, "academic_year", "Attendance");
        requireColumn(sheet, "subject_name", "Attendance");
        requireColumn(sheet, "academic_period_name", "Attendance");
        requireColumn(sheet, "session_date", "Attendance");
        requireColumn(sheet, "identification_number", "Attendance");
        requireColumn(sheet, "status", "Attendance");

        Map<Long, List<AttendanceCommands.RecordInput>> inputsBySession = new HashMap<>();
        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            ClassKey key = requiredClassKey(row, "Attendance", errors);
            LocalDate sessionDate = parseDate(row, "session_date", "Attendance", errors);
            String identification = required(row, "identification_number", 50, errors, "Attendance");
            AttendanceStatus status = parseStatus(row, "Attendance", errors);
            if (errors.size() > before || key == null) {
                failedKeys.add("Attendance#" + row.rowNumber());
                continue;
            }
            Long teachingPeriodId = resolveTeachingPeriod(row, "Attendance", teacherId, key, teachingPeriodCache, errors);
            if (teachingPeriodId == null) {
                failedKeys.add("Attendance#" + row.rowNumber());
                continue;
            }
            Long studentId = resolveStudentInPeriod(row, "Attendance", teachingPeriodId, identification,
                    studentByPeriodThenIdentification, errors);
            if (studentId == null) {
                failedKeys.add("Attendance#" + row.rowNumber());
                continue;
            }
            Long sessionId = resolveOrCreateSession(teacherId, teachingPeriodId, sessionDate, sessionByPeriodThenDate);
            if (sessionId == null) {
                errors.add(new ImportRowError(row.rowNumber(), "Attendance:session_date",
                        "Could not create the attendance session"));
                failedKeys.add("Attendance#" + row.rowNumber());
                continue;
            }
            inputsBySession.computeIfAbsent(sessionId, k -> new ArrayList<>())
                    .add(new AttendanceCommands.RecordInput(studentId, status, null));
        }
        inputsBySession.forEach((sessionId, inputs) -> {
            try {
                attendance.recordAttendance(new AttendanceCommands.RecordAttendance(teacherId, sessionId, inputs));
            } catch (RuntimeException e) {
                log.warn("Could not save attendance for session {}: {}", sessionId, e.getMessage());
                errors.add(new ImportRowError(0, "Attendance:session-" + sessionId, "Could not save: " + e.getMessage()));
            }
        });
        return sheet.rows().size();
    }

    private Long resolveOrCreateSession(Long teacherId, Long teachingPeriodId, LocalDate sessionDate,
                                        Map<Long, Map<LocalDate, Long>> cache) {
        Map<LocalDate, Long> byDate = cache.computeIfAbsent(teachingPeriodId, this::loadExistingSessions);
        Long sessionId = byDate.get(sessionDate);
        if (sessionId != null) {
            return sessionId;
        }
        try {
            AttendanceSessionView created = attendance.createSession(
                    new AttendanceCommands.CreateSession(teacherId, teachingPeriodId, sessionDate, null, null));
            byDate.put(sessionDate, created.sessionId());
            return created.sessionId();
        } catch (RuntimeException e) {
            log.warn("Could not create attendance session for teaching period {} on {}: {}", teachingPeriodId, sessionDate,
                    e.getMessage());
            return null;
        }
    }

    private Map<LocalDate, Long> loadExistingSessions(Long teachingPeriodId) {
        Map<LocalDate, Long> map = new HashMap<>();
        PageResult<AttendanceSessionView> page;
        int n = 0;
        do {
            page = sessions.findViewsByTeachingPeriodId(teachingPeriodId, new PageQuery(n++, PageQuery.MAX_SIZE));
            page.items().forEach(s -> map.put(s.sessionDate(), s.sessionId()));
        } while (n < page.totalPages());
        return map;
    }

    // ------------------------------------------------------------------ resolución compartida

    private ClassKey requiredClassKey(SpreadsheetRow row, String sheetLabel, List<ImportRowError> errors) {
        String gradeName = required(row, "grade_name", 50, errors, sheetLabel);
        String groupName = required(row, "group_name", 50, errors, sheetLabel);
        Integer academicYear = requiredYear(row, "academic_year", sheetLabel, errors);
        String subjectName = required(row, "subject_name", 100, errors, sheetLabel);
        String academicPeriodName = required(row, "academic_period_name", 100, errors, sheetLabel);
        if (gradeName == null || groupName == null || academicYear == null || subjectName == null
                || academicPeriodName == null) {
            return null;
        }
        return new ClassKey(gradeName, groupName, academicYear, subjectName, academicPeriodName);
    }

    private Long resolveTeachingPeriod(SpreadsheetRow row, String sheetLabel, Long teacherId, ClassKey key,
                                       Map<ClassKey, Long> cache, List<ImportRowError> errors) {
        Long cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<Group> group = groups.findByGradeNameAndNameAndAcademicYear(key.gradeName(), key.groupName(),
                key.academicYear());
        Optional<Subject> subject = subjects.findByName(key.subjectName());
        Optional<AcademicPeriod> period = academicPeriods.findByName(key.academicPeriodName());
        Optional<TeachingPeriod> teachingPeriod = Optional.empty();
        if (group.isPresent() && subject.isPresent() && period.isPresent()) {
            teachingPeriod = teachingAssignments
                    .findByTeacherIdAndGroupIdAndSubjectId(teacherId, group.get().getId(), subject.get().getId())
                    .flatMap(a -> teachingPeriods.findByTeachingAssignmentIdAndAcademicPeriodId(a.getId(), period.get().getId()));
        }
        if (teachingPeriod.isEmpty()) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":class",
                    "No class found for grade '" + key.gradeName() + "', group '" + key.groupName() + "' (" + key.academicYear()
                            + "), subject '" + key.subjectName() + "', period '" + key.academicPeriodName()
                            + "' (add it to the Classes sheet)"));
            return null;
        }
        cache.put(key, teachingPeriod.get().getId());
        return teachingPeriod.get().getId();
    }

    private Long resolveStudentInPeriod(SpreadsheetRow row, String sheetLabel, Long teachingPeriodId, String identification,
                                        Map<Long, Map<String, Long>> cache, List<ImportRowError> errors) {
        Long studentId = cache.computeIfAbsent(teachingPeriodId, this::loadRoster).get(identification);
        if (studentId == null) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":identification_number",
                    "No active student with this identification number in that class's group"));
            return null;
        }
        return studentId;
    }

    private Map<String, Long> loadRoster(Long teachingPeriodId) {
        TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
        return students.findActiveByGroupId(period.groupId()).stream()
                .filter(s -> s.getIdentificationNumber() != null)
                .collect(Collectors.toMap(Student::getIdentificationNumber, Student::getId, (a, b) -> a));
    }

    // ------------------------------------------------------------------ ayudantes de validación

    private static void requireColumn(ParsedSheet sheet, String column, String sheetLabel) {
        if (!sheet.headers().contains(column)) {
            throw new InvalidRequestException("MISSING_COLUMNS", "Sheet '" + sheetLabel + "' is missing required column: " + column);
        }
    }

    private static String required(SpreadsheetRow row, String column, int maxLength, List<ImportRowError> errors,
                                   String sheetLabel) {
        String value = row.get(column);
        if (value == null) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Required"));
            return null;
        }
        if (value.length() > maxLength) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Must have at most " + maxLength + " characters"));
            return null;
        }
        return value;
    }

    private static String optional(SpreadsheetRow row, String column, int maxLength, List<ImportRowError> errors,
                                   String sheetLabel) {
        String value = row.get(column);
        if (value != null && value.length() > maxLength) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Must have at most " + maxLength + " characters"));
            return null;
        }
        return value;
    }

    private static Integer requiredYear(SpreadsheetRow row, String column, String sheetLabel, List<ImportRowError> errors) {
        String raw = row.get(column);
        if (raw == null) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Required"));
            return null;
        }
        try {
            int year = Integer.parseInt(raw);
            if (year < 2000 || year > 2200) {
                throw new NumberFormatException();
            }
            return year;
        } catch (NumberFormatException e) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Must be an integer year between 2000 and 2200"));
            return null;
        }
    }

    private static BigDecimal requiredPositiveDecimal(SpreadsheetRow row, String column, String sheetLabel,
                                                       List<ImportRowError> errors) {
        String raw = row.get(column);
        if (raw == null) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Required"));
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.signum() <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Must be a positive number"));
            return null;
        }
    }

    private static LocalDate parseDate(SpreadsheetRow row, String column, String sheetLabel, List<ImportRowError> errors) {
        String raw = row.get(column);
        if (raw == null) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Required"));
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column, "Must be a date in yyyy-MM-dd format"));
            return null;
        }
    }

    private static LocalDateTime parseDateTimeOrNull(SpreadsheetRow row, String column, String sheetLabel,
                                                      List<ImportRowError> errors) {
        String raw = row.get(column);
        if (raw == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":" + column,
                    "Must be a date-time in yyyy-MM-dd HH:mm format"));
            return null;
        }
    }

    private static AttendanceStatus parseStatus(SpreadsheetRow row, String sheetLabel, List<ImportRowError> errors) {
        String raw = row.get("status");
        if (raw == null) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":status", "Required"));
            return null;
        }
        try {
            return AttendanceStatus.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.add(new ImportRowError(row.rowNumber(), sheetLabel + ":status", "Must be PRESENT, ABSENT or EXCUSED"));
            return null;
        }
    }

    // ------------------------------------------------------------------ cierre del batch

    private ImportResult finish(ImportBatch batch, Long userId, int total, int successful, List<ImportRowError> errors,
                                int failedRows) {
        batch.setTotalRows(total);
        batch.setSuccessfulRows(successful);
        batch.setFailedRows(failedRows);
        batch.setStatus(failedRows == 0 ? ImportStatus.COMPLETED
                : successful > 0 ? ImportStatus.COMPLETED_WITH_ERRORS : ImportStatus.FAILED);
        batch.setCompletedAt(LocalDateTime.now(clock));
        if (!errors.isEmpty()) {
            List<List<Object>> lines = errors.stream()
                    .map(e -> List.<Object>of(e.rowNumber(), e.column() == null ? "" : e.column(), e.message())).toList();
            batch.setErrorReportPath(storage.store("imports/errors", "import-errors.xlsx",
                    writer.write(new TabularData("Errors", List.of("row", "column", "error"), lines))));
        }
        ImportBatch saved = batches.save(batch);
        audit.success(userId, AuditAction.IMPORT, "ImportBatch", saved.getId(),
                "rows " + total + ", ok " + successful + ", failed " + failedRows);
        boolean truncated = errors.size() > MAX_RETURNED_ERRORS;
        return new ImportResult(saved, truncated ? errors.subList(0, MAX_RETURNED_ERRORS) : errors, truncated);
    }

    private void fail(ImportBatch batch, Long userId, RuntimeException cause) {
        log.warn("School setup import {} failed: {}", batch.getId(), cause.getMessage());
        try {
            batch.setStatus(ImportStatus.FAILED);
            batch.setCompletedAt(LocalDateTime.now(clock));
            batches.save(batch);
        } catch (RuntimeException e) {
            log.error("Could not mark import batch {} as failed", batch.getId(), e);
        }
        audit.failure(userId, AuditAction.IMPORT, "ImportBatch", batch.getId(), cause.getMessage());
    }

    private static boolean looksLikeZip(byte[] content) {
        return content != null && content.length > 4 && content[0] == 'P' && content[1] == 'K';
    }
}
