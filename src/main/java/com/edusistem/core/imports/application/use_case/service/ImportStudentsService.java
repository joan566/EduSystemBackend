package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.domain.inputports.ImportStudentsUseCase;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.imports.domain.vo.ImportResult;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import com.edusistem.core.shared.domain.outputports.OwnershipPort;
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importación de estudiantes desde Excel. Una fila incorrecta no aborta la importación: se valida todo, se aplican
 * (en una sola transacción) las filas válidas y se registra el resultado en import_batches con el detalle de errores.
 * Este servicio no es transaccional a propósito: el batch debe poder quedar registrado como FAILED aunque la
 * transacción de aplicación se revierta.
 */
public class ImportStudentsService implements ImportStudentsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportStudentsService.class);

    static final List<String> REQUIRED_COLUMNS = List.of("identification_number", "first_name", "last_name", "grade", "group");
    static final List<String> TEMPLATE_COLUMNS = List.of("identification_number", "first_name", "last_name", "email",
            "grade", "group", "academic_year");
    private static final int MAX_ROWS = 5000;
    private static final int MAX_RETURNED_ERRORS = 500;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SpreadsheetReaderPort reader;
    private final SpreadsheetWriterPort writer;
    private final ImportBatchRepositoryPort batches;
    private final StudentRepositoryPort students;
    private final GradeRepositoryPort grades;
    private final GroupRepositoryPort groups;
    private final OwnershipPort ownership;
    private final FileStoragePort storage;
    private final StudentImportApplier applier;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    public ImportStudentsService(SpreadsheetReaderPort reader, SpreadsheetWriterPort writer,
                                 ImportBatchRepositoryPort batches, StudentRepositoryPort students,
                                 GradeRepositoryPort grades, GroupRepositoryPort groups, OwnershipPort ownership,
                                 FileStoragePort storage, StudentImportApplier applier, RecordAuditUseCase audit,
                                 Clock clock) {
        this.reader = reader;
        this.writer = writer;
        this.batches = batches;
        this.students = students;
        this.grades = grades;
        this.groups = groups;
        this.ownership = ownership;
        this.storage = storage;
        this.applier = applier;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public ImportResult importStudents(ImportCommands.ImportStudents command) {
        String fileName = command.fileName() == null ? "students.xlsx" : command.fileName();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx") || !looksLikeZip(command.content())) {
            throw new InvalidRequestException("INVALID_FILE_TYPE", "Only Excel .xlsx files are supported");
        }
        ImportBatch batch = batches.save(ImportBatch.builder().userId(command.teacherId())
                .fileName(fileName.length() > 255 ? fileName.substring(0, 255) : fileName)
                .filePath(storage.store("imports", fileName, command.content()))
                .status(ImportStatus.PROCESSING).build());
        try {
            ParsedSheet sheet = reader.read(command.content());
            requireColumns(sheet);
            if (sheet.rows().size() > MAX_ROWS) {
                throw new InvalidRequestException("TOO_MANY_ROWS", "The file has more than " + MAX_ROWS + " rows");
            }
            List<ImportRowError> errors = new ArrayList<>();
            List<StudentImportRow> valid = validate(command.teacherId(), sheet.rows(), errors);
            applier.apply(valid);
            return finish(batch, command.teacherId(), sheet.rows().size(), valid.size(), errors);
        } catch (RuntimeException e) {
            fail(batch, command.teacherId(), e);
            throw e;
        }
    }

    @Override
    public byte[] template() {
        return writer.write(new TabularData("Students", TEMPLATE_COLUMNS, List.of(
                List.<Object>of("1001234567", "Ana", "Pérez", "ana.perez@example.com", "10°", "A", 2026))));
    }

    // ------------------------------------------------------------------ validación

    private void requireColumns(ParsedSheet sheet) {
        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !sheet.headers().contains(c)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException("MISSING_COLUMNS",
                    "Missing required columns: " + String.join(", ", missing) + ". Expected: " + String.join(", ", TEMPLATE_COLUMNS));
        }
    }

    private List<StudentImportRow> validate(Long teacherId, List<SpreadsheetRow> rows, List<ImportRowError> errors) {
        List<StudentImportRow> valid = new ArrayList<>();
        Map<String, Integer> seenIdentifications = new HashMap<>();
        Map<String, Integer> seenCodes = new HashMap<>();
        Map<String, Optional<Long>> groupCache = new HashMap<>();
        Map<Long, Boolean> teachesCache = new HashMap<>();
        int currentYear = LocalDateTime.now(clock).getYear();

        for (SpreadsheetRow row : rows) {
            int before = errors.size();
            String identification = required(row, "identification_number", 50, errors);
            String firstName = required(row, "first_name", 100, errors);
            String lastName = required(row, "last_name", 100, errors);
            String gradeName = required(row, "grade", 50, errors);
            String groupName = required(row, "group", 50, errors);
            String email = row.get("email");
            String code = row.get("student_code");
            if (email != null && (email.length() > 255 || !EMAIL.matcher(email).matches())) {
                errors.add(new ImportRowError(row.rowNumber(), "email", "Invalid e-mail address"));
            }
            if (code != null && code.length() > 50) {
                errors.add(new ImportRowError(row.rowNumber(), "student_code", "Must have at most 50 characters"));
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
                    errors.add(new ImportRowError(row.rowNumber(), "academic_year", "Must be an integer year between 2000 and 2200"));
                }
            }
            if (identification != null) {
                Integer first = seenIdentifications.putIfAbsent(identification, row.rowNumber());
                if (first != null) {
                    errors.add(new ImportRowError(row.rowNumber(), "identification_number",
                            "Duplicated in the file (first seen in row " + first + ")"));
                }
            }
            if (code != null) {
                Integer first = seenCodes.putIfAbsent(code, row.rowNumber());
                if (first != null) {
                    errors.add(new ImportRowError(row.rowNumber(), "student_code",
                            "Duplicated in the file (first seen in row " + first + ")"));
                }
            }
            Long groupId = null;
            if (gradeName != null && groupName != null && errors.size() == before) {
                groupId = resolveGroup(row, gradeName, groupName, year, groupCache, teachesCache, teacherId, errors);
            }
            if (errors.size() > before) {
                continue;
            }
            resolveStudent(row, teacherId, identification, code, firstName, lastName, email, groupId, errors)
                    .ifPresent(valid::add);
        }
        return valid;
    }

    private Long resolveGroup(SpreadsheetRow row, String gradeName, String groupName, int year,
                              Map<String, Optional<Long>> cache, Map<Long, Boolean> teachesCache, Long teacherId,
                              List<ImportRowError> errors) {
        String key = gradeName + "|" + groupName + "|" + year;
        Optional<Long> groupId = cache.computeIfAbsent(key,
                k -> groups.findByGradeNameAndNameAndAcademicYear(gradeName, groupName, year).map(Group::getId));
        if (groupId.isEmpty()) {
            boolean gradeExists = grades.findByName(gradeName).isPresent();
            errors.add(new ImportRowError(row.rowNumber(), gradeExists ? "group" : "grade", gradeExists
                    ? "Group '" + groupName + "' does not exist in grade '" + gradeName + "' for " + year
                    : "Grade '" + gradeName + "' does not exist"));
            return null;
        }
        boolean teaches = teachesCache.computeIfAbsent(groupId.get(), id -> ownership.teachesGroup(teacherId, id));
        if (!teaches) {
            errors.add(new ImportRowError(row.rowNumber(), "group",
                    "You have no teaching assignment for group " + gradeName + " " + groupName + " (" + year + ")"));
            return null;
        }
        return groupId.get();
    }

    /** Decide crear o reutilizar al estudiante; solo actualiza sus datos si el profesor ya puede verlo. */
    private Optional<StudentImportRow> resolveStudent(SpreadsheetRow row, Long teacherId, String identification,
                                                      String code, String firstName, String lastName, String email,
                                                      Long groupId, List<ImportRowError> errors) {
        Optional<Student> byIdentification = students.findByIdentificationNumber(identification);
        Optional<Student> byCode = code == null ? Optional.empty() : students.findByStudentCode(code);
        if (byIdentification.isPresent() && byCode.isPresent() && !byIdentification.get().getId().equals(byCode.get().getId())) {
            errors.add(new ImportRowError(row.rowNumber(), "student_code",
                    "The student code belongs to a different student than the identification number"));
            return Optional.empty();
        }
        if (byIdentification.isEmpty() && byCode.isPresent()) {
            errors.add(new ImportRowError(row.rowNumber(), "student_code", "The student code is already used by another student"));
            return Optional.empty();
        }
        Long existingId = byIdentification.map(Student::getId).orElse(null);
        boolean update = existingId != null && ownership.teachesStudent(teacherId, existingId);
        return Optional.of(new StudentImportRow(row.rowNumber(), existingId, update, identification, code, firstName,
                lastName, email, groupId));
    }

    private static String required(SpreadsheetRow row, String column, int maxLength, List<ImportRowError> errors) {
        String value = row.get(column);
        if (value == null) {
            errors.add(new ImportRowError(row.rowNumber(), column, "Required"));
            return null;
        }
        if (value.length() > maxLength) {
            errors.add(new ImportRowError(row.rowNumber(), column, "Must have at most " + maxLength + " characters"));
            return null;
        }
        return value;
    }

    // ------------------------------------------------------------------ cierre del batch

    private ImportResult finish(ImportBatch batch, Long userId, int total, int successful, List<ImportRowError> errors) {
        int failedRows = (int) errors.stream().map(ImportRowError::rowNumber).distinct().count();
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
        log.warn("Student import {} failed: {}", batch.getId(), cause.getMessage());
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
