package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
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
import com.edusistem.core.imports.domain.inputports.ImportTeachingPeriodDataUseCase;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.imports.domain.vo.ImportResult;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.ParsedWorkbook;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.domain.vo.PeriodWorkbookColumns;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importación combinada de un teaching period: hojas Estudiantes, Notas y Asistencia, todas opcionales (también se
 * aceptan los nombres y encabezados en inglés de los archivos antiguos, ver {@link SpreadsheetVocabulary}). Las columnas
 * dinámicas de Notas/Asistencia se reconocen por el id incrustado en su encabezado (ver {@link PeriodWorkbookColumns}),
 * generado por {@code GET /exports/teaching-periods/{id}/full}, que también sirve de plantilla. Cada fila se valida
 * antes de aplicarse: una fila o celda incorrecta no aborta el resto de la importación. Las notas y la asistencia se
 * registran a través de los mismos casos de uso que sus endpoints dedicados (respetan las mismas reglas de negocio:
 * 0 ≤ nota ≤ puntaje máximo, estudiante matriculado en el grupo del periodo).
 */
public class ImportTeachingPeriodDataService implements ImportTeachingPeriodDataUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportTeachingPeriodDataService.class);
    private static final int MAX_RETURNED_ERRORS = 500;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SpreadsheetReaderPort reader;
    private final SpreadsheetWriterPort writer;
    private final ImportBatchRepositoryPort batches;
    private final StudentRepositoryPort students;
    private final OwnershipGuard guard;
    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final ActivityRepositoryPort activities;
    private final GradeActivityUseCase gradeActivity;
    private final AttendanceSessionRepositoryPort sessions;
    private final ManageAttendanceUseCase attendance;
    private final StudentImportApplier applier;
    private final FileStoragePort storage;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    public ImportTeachingPeriodDataService(SpreadsheetReaderPort reader, SpreadsheetWriterPort writer,
                                           ImportBatchRepositoryPort batches, StudentRepositoryPort students,
                                           OwnershipGuard guard,
                                           TeachingPeriodRepositoryPort teachingPeriods, ActivityRepositoryPort activities,
                                           GradeActivityUseCase gradeActivity, AttendanceSessionRepositoryPort sessions,
                                           ManageAttendanceUseCase attendance, StudentImportApplier applier,
                                           FileStoragePort storage, RecordAuditUseCase audit, Clock clock) {
        this.reader = reader;
        this.writer = writer;
        this.batches = batches;
        this.students = students;
        this.guard = guard;
        this.teachingPeriods = teachingPeriods;
        this.activities = activities;
        this.gradeActivity = gradeActivity;
        this.sessions = sessions;
        this.attendance = attendance;
        this.applier = applier;
        this.storage = storage;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public ImportResult importData(ImportCommands.ImportTeachingPeriodData command) {
        guard.requireTeachingPeriod(command.teacherId(), command.teachingPeriodId());
        TeachingPeriodView period = teachingPeriods.findViewById(command.teachingPeriodId())
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", command.teachingPeriodId()));

        String fileName = command.fileName() == null ? "teaching-period.xlsx" : command.fileName();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx") || !looksLikeZip(command.content())) {
            throw new InvalidRequestException("INVALID_FILE_TYPE", ImportMessages.ONLY_XLSX);
        }
        ImportBatch batch = batches.save(ImportBatch.builder().userId(command.teacherId())
                .fileName(fileName.length() > 255 ? fileName.substring(0, 255) : fileName)
                .filePath(storage.store("imports", fileName, command.content()))
                .status(ImportStatus.PROCESSING).build());
        try {
            ParsedWorkbook workbook = reader.readAll(command.content());
            List<ImportRowError> errors = new ArrayList<>();
            Set<String> failedKeys = new HashSet<>();

            int totalRows = applyStudents(command.teacherId(), period.groupId(), workbook, errors, failedKeys);
            Map<String, Long> studentIdByIdentification = students.findActiveByGroupId(period.groupId()).stream()
                    .filter(s -> s.getIdentificationNumber() != null)
                    .collect(Collectors.toMap(Student::getIdentificationNumber, Student::getId, (a, b) -> a));
            totalRows += applyGrades(command.teacherId(), command.teachingPeriodId(), workbook, studentIdByIdentification,
                    errors, failedKeys);
            totalRows += applyAttendance(command.teacherId(), command.teachingPeriodId(), workbook, studentIdByIdentification,
                    errors, failedKeys);

            return finish(batch, command.teacherId(), totalRows, totalRows - failedKeys.size(), errors, failedKeys.size());
        } catch (RuntimeException e) {
            fail(batch, command.teacherId(), e);
            throw e;
        }
    }

    // ------------------------------------------------------------------ Students

    private int applyStudents(Long teacherId, Long groupId, ParsedWorkbook workbook, List<ImportRowError> errors,
                              Set<String> failedKeys) {
        SheetSpec spec = SpreadsheetVocabulary.STUDENTS;
        Optional<ParsedSheet> sheetOpt = workbook.sheet(spec);
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "identification_number", spec);
        requireColumn(sheet, "first_name", spec);
        requireColumn(sheet, "last_name", spec);

        List<StudentImportRow> valid = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (SpreadsheetRow row : sheet.rows()) {
            int before = errors.size();
            String identification = required(row, "identification_number", 50, errors, spec);
            String firstName = required(row, "first_name", 100, errors, spec);
            String lastName = required(row, "last_name", 100, errors, spec);
            String email = row.get("email");
            if (email != null && (email.length() > 255 || !EMAIL.matcher(email).matches())) {
                errors.add(new ImportRowError(row.rowNumber(), spec.ref("email"), ImportMessages.INVALID_EMAIL));
            }
            if (identification != null) {
                Integer first = seen.putIfAbsent(identification, row.rowNumber());
                if (first != null) {
                    errors.add(new ImportRowError(row.rowNumber(), spec.ref("identification_number"),
                            ImportMessages.duplicatedInSheet(first)));
                }
            }
            if (errors.size() > before) {
                failedKeys.add("Students#" + row.rowNumber());
                continue;
            }
            Long existingId = students.findByTeacherIdAndIdentificationNumber(teacherId, identification)
                    .map(Student::getId).orElse(null);
            valid.add(new StudentImportRow(row.rowNumber(), existingId, identification, null, firstName, lastName,
                    email, groupId));
        }
        applier.apply(teacherId, valid);
        return sheet.rows().size();
    }

    // ------------------------------------------------------------------ Grades

    private int applyGrades(Long teacherId, Long teachingPeriodId, ParsedWorkbook workbook,
                            Map<String, Long> studentIdByIdentification, List<ImportRowError> errors, Set<String> failedKeys) {
        SheetSpec spec = SpreadsheetVocabulary.GRADES;
        Optional<ParsedSheet> sheetOpt = workbook.sheet(spec);
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "identification_number", spec);

        Map<Long, ActivityView> activityById = allActivities(teachingPeriodId).stream()
                .collect(Collectors.toMap(ActivityView::activityId, a -> a));
        Map<String, ActivityView> activityByHeader = columnsById(sheet, activityById);

        Map<Long, List<ActivityCommands.GradeInput>> inputsByActivity = new HashMap<>();
        Set<String> seenIdentification = new HashSet<>();
        for (SpreadsheetRow row : sheet.rows()) {
            Long studentId = resolveRosterStudent(row, spec, studentIdByIdentification, seenIdentification, errors, failedKeys);
            if (studentId == null) {
                continue;
            }
            boolean rowFailed = false;
            for (Map.Entry<String, ActivityView> entry : activityByHeader.entrySet()) {
                String raw = row.get(entry.getKey());
                if (raw == null) {
                    continue;
                }
                ActivityView activity = entry.getValue();
                BigDecimal grade;
                try {
                    grade = new BigDecimal(raw);
                } catch (NumberFormatException e) {
                    errors.add(new ImportRowError(row.rowNumber(), spec.ref(activity.name()), ImportMessages.NOT_A_NUMBER));
                    rowFailed = true;
                    continue;
                }
                if (grade.signum() < 0 || grade.compareTo(activity.maximumScore()) > 0) {
                    errors.add(new ImportRowError(row.rowNumber(), spec.ref(activity.name()),
                            ImportMessages.between(activity.maximumScore())));
                    rowFailed = true;
                    continue;
                }
                inputsByActivity.computeIfAbsent(activity.activityId(), k -> new ArrayList<>())
                        .add(new ActivityCommands.GradeInput(studentId, grade, null));
            }
            if (rowFailed) {
                failedKeys.add("Grades#" + row.rowNumber());
            }
        }
        inputsByActivity.forEach((activityId, inputs) -> {
            try {
                gradeActivity.recordGrades(new ActivityCommands.RecordGrades(teacherId, activityId, inputs));
            } catch (RuntimeException e) {
                log.warn("Could not save grades for activity {}: {}", activityId, e.getMessage());
                errors.add(new ImportRowError(0, spec.ref(activityById.get(activityId).name()),
                        ImportMessages.couldNotSave(e.getMessage())));
            }
        });
        return sheet.rows().size();
    }

    // ------------------------------------------------------------------ Attendance

    private int applyAttendance(Long teacherId, Long teachingPeriodId, ParsedWorkbook workbook,
                                Map<String, Long> studentIdByIdentification, List<ImportRowError> errors, Set<String> failedKeys) {
        SheetSpec spec = SpreadsheetVocabulary.ATTENDANCE;
        Optional<ParsedSheet> sheetOpt = workbook.sheet(spec);
        if (sheetOpt.isEmpty()) {
            return 0;
        }
        ParsedSheet sheet = sheetOpt.get();
        requireColumn(sheet, "identification_number", spec);

        Map<Long, AttendanceSessionView> sessionById = allSessions(teachingPeriodId).stream()
                .collect(Collectors.toMap(AttendanceSessionView::sessionId, s -> s));
        Map<String, AttendanceSessionView> sessionByHeader = columnsById(sheet, sessionById);

        Map<Long, List<AttendanceCommands.RecordInput>> inputsBySession = new HashMap<>();
        Set<String> seenIdentification = new HashSet<>();
        for (SpreadsheetRow row : sheet.rows()) {
            Long studentId = resolveRosterStudent(row, spec, studentIdByIdentification, seenIdentification, errors, failedKeys);
            if (studentId == null) {
                continue;
            }
            boolean rowFailed = false;
            for (Map.Entry<String, AttendanceSessionView> entry : sessionByHeader.entrySet()) {
                String raw = row.get(entry.getKey());
                if (raw == null) {
                    continue;
                }
                AttendanceSessionView session = entry.getValue();
                Optional<AttendanceStatus> status = SpreadsheetVocabulary.attendanceStatusName(raw).map(AttendanceStatus::valueOf);
                if (status.isEmpty()) {
                    errors.add(new ImportRowError(row.rowNumber(), spec.ref(session.sessionDate().toString()),
                            ImportMessages.INVALID_ATTENDANCE));
                    rowFailed = true;
                    continue;
                }
                inputsBySession.computeIfAbsent(session.sessionId(), k -> new ArrayList<>())
                        .add(new AttendanceCommands.RecordInput(studentId, status.get(), null));
            }
            if (rowFailed) {
                failedKeys.add("Attendance#" + row.rowNumber());
            }
        }
        inputsBySession.forEach((sessionId, inputs) -> {
            try {
                attendance.recordAttendance(new AttendanceCommands.RecordAttendance(teacherId, sessionId, inputs));
            } catch (RuntimeException e) {
                log.warn("Could not save attendance for session {}: {}", sessionId, e.getMessage());
                errors.add(new ImportRowError(0, spec.ref(sessionById.get(sessionId).sessionDate().toString()),
                        ImportMessages.couldNotSave(e.getMessage())));
            }
        });
        return sheet.rows().size();
    }

    private Long resolveRosterStudent(SpreadsheetRow row, SheetSpec spec, Map<String, Long> studentIdByIdentification,
                                      Set<String> seenIdentification, List<ImportRowError> errors, Set<String> failedKeys) {
        String identification = row.get("identification_number");
        if (identification == null) {
            errors.add(new ImportRowError(row.rowNumber(), spec.ref("identification_number"), ImportMessages.REQUIRED));
            failedKeys.add(spec.name() + "#" + row.rowNumber());
            return null;
        }
        if (!seenIdentification.add(identification)) {
            errors.add(new ImportRowError(row.rowNumber(), spec.ref("identification_number"), ImportMessages.DUPLICATED_IN_SHEET));
            failedKeys.add(spec.name() + "#" + row.rowNumber());
            return null;
        }
        Long studentId = studentIdByIdentification.get(identification);
        if (studentId == null) {
            errors.add(new ImportRowError(row.rowNumber(), spec.ref("identification_number"),
                    "No hay un estudiante activo con este número de identificación en el grupo"));
            failedKeys.add(spec.name() + "#" + row.rowNumber());
            return null;
        }
        return studentId;
    }

    // ------------------------------------------------------------------ ayudantes

    private List<ActivityView> allActivities(Long teachingPeriodId) {
        List<ActivityView> list = new ArrayList<>();
        PageResult<ActivityView> page;
        int n = 0;
        do {
            page = activities.findViewsByTeachingPeriodId(teachingPeriodId, new PageQuery(n++, PageQuery.MAX_SIZE));
            list.addAll(page.items());
        } while (n < page.totalPages());
        return list;
    }

    private List<AttendanceSessionView> allSessions(Long teachingPeriodId) {
        List<AttendanceSessionView> list = new ArrayList<>();
        PageResult<AttendanceSessionView> page;
        int n = 0;
        do {
            page = sessions.findViewsByTeachingPeriodId(teachingPeriodId, new PageQuery(n++, PageQuery.MAX_SIZE));
            list.addAll(page.items());
        } while (n < page.totalPages());
        return list;
    }

    /**
     * Columnas dinámicas de la hoja (encabezado normalizado → actividad/sesión) según el id incrustado. Solo se tienen
     * en cuenta los ids que pertenecen al teaching period; si dos columnas traen el mismo id, vale la primera.
     */
    private static <T> Map<String, T> columnsById(ParsedSheet sheet, Map<Long, T> byId) {
        Map<String, T> byHeader = new LinkedHashMap<>();
        Set<Long> used = new HashSet<>();
        for (String header : sheet.headers()) {
            PeriodWorkbookColumns.idFromHeader(header)
                    .filter(byId::containsKey)
                    .filter(used::add)
                    .ifPresent(id -> byHeader.put(header, byId.get(id)));
        }
        return byHeader;
    }

    private static void requireColumn(ParsedSheet sheet, String column, SheetSpec spec) {
        if (!sheet.headers().contains(column)) {
            throw new InvalidRequestException("MISSING_COLUMNS", ImportMessages.missingColumn(spec.label(), spec.header(column)));
        }
    }

    private static String required(SpreadsheetRow row, String column, int maxLength, List<ImportRowError> errors, SheetSpec spec) {
        String value = row.get(column);
        if (value == null) {
            errors.add(new ImportRowError(row.rowNumber(), spec.ref(column), ImportMessages.REQUIRED));
            return null;
        }
        if (value.length() > maxLength) {
            errors.add(new ImportRowError(row.rowNumber(), spec.ref(column), ImportMessages.maxLength(maxLength)));
            return null;
        }
        return value;
    }

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
                    writer.write(SpreadsheetVocabulary.ERRORS.table(List.of("row", "column", "error"), lines))));
        }
        ImportBatch saved = batches.save(batch);
        audit.success(userId, AuditAction.IMPORT, "ImportBatch", saved.getId(),
                "rows " + total + ", ok " + successful + ", failed " + failedRows);
        boolean truncated = errors.size() > MAX_RETURNED_ERRORS;
        return new ImportResult(saved, truncated ? errors.subList(0, MAX_RETURNED_ERRORS) : errors, truncated);
    }

    private void fail(ImportBatch batch, Long userId, RuntimeException cause) {
        log.warn("Teaching period data import {} failed: {}", batch.getId(), cause.getMessage());
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
