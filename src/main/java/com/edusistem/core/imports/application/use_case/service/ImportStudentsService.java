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
import com.edusistem.core.shared.domain.outputports.SpreadsheetWriterPort;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
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
    /** Encabezados en español; al leer también se aceptan las claves en inglés de las plantillas antiguas. */
    static final SheetSpec SHEET = new SheetSpec("Students", "Estudiantes", Map.of(
            "identification_number", "Número de identificación",
            "student_code", "Código",
            "first_name", "Nombres",
            "last_name", "Apellidos",
            "email", "Correo electrónico",
            "grade", "Grado",
            "group", "Grupo",
            "academic_year", "Año lectivo"));
    private static final int MAX_ROWS = 5000;
    private static final int MAX_RETURNED_ERRORS = 500;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SpreadsheetReaderPort reader;
    private final SpreadsheetWriterPort writer;
    private final ImportBatchRepositoryPort batches;
    private final StudentRepositoryPort students;
    private final GradeRepositoryPort grades;
    private final GroupRepositoryPort groups;
    private final FileStoragePort storage;
    private final StudentImportApplier applier;
    private final RecordAuditUseCase audit;
    private final Clock clock;

    public ImportStudentsService(SpreadsheetReaderPort reader, SpreadsheetWriterPort writer,
                                 ImportBatchRepositoryPort batches, StudentRepositoryPort students,
                                 GradeRepositoryPort grades, GroupRepositoryPort groups,
                                 FileStoragePort storage, StudentImportApplier applier, RecordAuditUseCase audit,
                                 Clock clock) {
        this.reader = reader;
        this.writer = writer;
        this.batches = batches;
        this.students = students;
        this.grades = grades;
        this.groups = groups;
        this.storage = storage;
        this.applier = applier;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public ImportResult importStudents(ImportCommands.ImportStudents command) {
        String fileName = command.fileName() == null ? "students.xlsx" : command.fileName();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx") || !looksLikeZip(command.content())) {
            throw new InvalidRequestException("INVALID_FILE_TYPE", ImportMessages.ONLY_XLSX);
        }
        ImportBatch batch = batches.save(ImportBatch.builder().userId(command.teacherId())
                .fileName(fileName.length() > 255 ? fileName.substring(0, 255) : fileName)
                .filePath(storage.store("imports", fileName, command.content()))
                .status(ImportStatus.PROCESSING).build());
        try {
            ParsedSheet sheet = reader.read(command.content()).canonicalize(SHEET);
            requireColumns(sheet);
            if (sheet.rows().size() > MAX_ROWS) {
                throw new InvalidRequestException("TOO_MANY_ROWS", ImportMessages.tooManyRows(MAX_ROWS));
            }
            List<ImportRowError> errors = new ArrayList<>();
            List<StudentImportRow> valid = validate(command.teacherId(), sheet.rows(), errors);
            applier.apply(command.teacherId(), valid);
            return finish(batch, command.teacherId(), sheet.rows().size(), valid.size(), errors);
        } catch (RuntimeException e) {
            fail(batch, command.teacherId(), e);
            throw e;
        }
    }

    @Override
    public byte[] template() {
        return writer.write(SHEET.table(TEMPLATE_COLUMNS, List.of(
                List.<Object>of("1001234567", "Ana", "Pérez", "ana.perez@example.com", "10°", "A", 2026))));
    }

    // ------------------------------------------------------------------ validación

    private void requireColumns(ParsedSheet sheet) {
        List<String> missing = REQUIRED_COLUMNS.stream().filter(c -> !sheet.headers().contains(c)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException("MISSING_COLUMNS",
                    "Faltan columnas obligatorias: " + String.join(", ", SHEET.headers(missing))
                            + ". Columnas esperadas: " + String.join(", ", SHEET.headers(TEMPLATE_COLUMNS)));
        }
    }

    private List<StudentImportRow> validate(Long teacherId, List<SpreadsheetRow> rows, List<ImportRowError> errors) {
        List<StudentImportRow> valid = new ArrayList<>();
        Map<String, Integer> seenIdentifications = new HashMap<>();
        Map<String, Integer> seenCodes = new HashMap<>();
        Map<String, Optional<Long>> groupCache = new HashMap<>();
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
                errors.add(new ImportRowError(row.rowNumber(), SHEET.header("email"), ImportMessages.INVALID_EMAIL));
            }
            if (code != null && code.length() > 50) {
                errors.add(new ImportRowError(row.rowNumber(), SHEET.header("student_code"), ImportMessages.maxLength(50)));
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
                    errors.add(new ImportRowError(row.rowNumber(), SHEET.header("academic_year"), ImportMessages.INVALID_YEAR));
                }
            }
            if (identification != null) {
                Integer first = seenIdentifications.putIfAbsent(identification, row.rowNumber());
                if (first != null) {
                    errors.add(new ImportRowError(row.rowNumber(), SHEET.header("identification_number"),
                            ImportMessages.duplicatedInFile(first)));
                }
            }
            if (code != null) {
                Integer first = seenCodes.putIfAbsent(code, row.rowNumber());
                if (first != null) {
                    errors.add(new ImportRowError(row.rowNumber(), SHEET.header("student_code"),
                            ImportMessages.duplicatedInFile(first)));
                }
            }
            Long groupId = null;
            if (gradeName != null && groupName != null && errors.size() == before) {
                groupId = resolveGroup(row, gradeName, groupName, year, groupCache, teacherId, errors);
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
                              Map<String, Optional<Long>> cache, Long teacherId, List<ImportRowError> errors) {
        String key = gradeName + "|" + groupName + "|" + year;
        Optional<Long> groupId = cache.computeIfAbsent(key,
                k -> groups.findByGradeNameAndNameAndAcademicYear(teacherId, gradeName, groupName, year).map(Group::getId));
        if (groupId.isEmpty()) {
            boolean gradeExists = grades.findByTeacherIdAndName(teacherId, gradeName).isPresent();
            errors.add(new ImportRowError(row.rowNumber(), SHEET.header(gradeExists ? "group" : "grade"), gradeExists
                    ? "El grupo '" + groupName + "' no existe en el grado '" + gradeName + "' para " + year
                    : "El grado '" + gradeName + "' no existe"));
            return null;
        }
        return groupId.get();
    }

    /** Decide crear o reutilizar (y actualizar) a un estudiante del propio profesor. */
    private Optional<StudentImportRow> resolveStudent(SpreadsheetRow row, Long teacherId, String identification,
                                                      String code, String firstName, String lastName, String email,
                                                      Long groupId, List<ImportRowError> errors) {
        Optional<Student> byIdentification = students.findByTeacherIdAndIdentificationNumber(teacherId, identification);
        Optional<Student> byCode = code == null ? Optional.empty() : students.findByTeacherIdAndStudentCode(teacherId, code);
        if (byIdentification.isPresent() && byCode.isPresent() && !byIdentification.get().getId().equals(byCode.get().getId())) {
            errors.add(new ImportRowError(row.rowNumber(), SHEET.header("student_code"),
                    "El código pertenece a un estudiante distinto al del número de identificación"));
            return Optional.empty();
        }
        if (byIdentification.isEmpty() && byCode.isPresent()) {
            errors.add(new ImportRowError(row.rowNumber(), SHEET.header("student_code"), "El código ya lo usa otro estudiante"));
            return Optional.empty();
        }
        Long existingId = byIdentification.map(Student::getId).orElse(null);
        return Optional.of(new StudentImportRow(row.rowNumber(), existingId, identification, code, firstName,
                lastName, email, groupId));
    }

    private static String required(SpreadsheetRow row, String column, int maxLength, List<ImportRowError> errors) {
        String value = row.get(column);
        if (value == null) {
            errors.add(new ImportRowError(row.rowNumber(), SHEET.header(column), ImportMessages.REQUIRED));
            return null;
        }
        if (value.length() > maxLength) {
            errors.add(new ImportRowError(row.rowNumber(), SHEET.header(column), ImportMessages.maxLength(maxLength)));
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
                    writer.write(SpreadsheetVocabulary.ERRORS.table(List.of("row", "column", "error"), lines))));
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
