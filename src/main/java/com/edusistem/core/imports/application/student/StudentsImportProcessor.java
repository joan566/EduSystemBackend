package com.edusistem.core.imports.application.student;

import static com.edusistem.core.imports.application.student.StudentImportSheet.REQUIRED_COLUMNS;
import static com.edusistem.core.imports.application.student.StudentImportSheet.SHEET;
import static com.edusistem.core.imports.application.student.StudentImportSheet.TEMPLATE_COLUMNS;

import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.imports.application.contracts.ImportOutcome;
import com.edusistem.core.imports.application.contracts.ImportProcessor;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.ImportProgress;
import com.edusistem.core.imports.application.support.RowErrors;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;
import com.edusistem.core.imports.domain.outputports.SpreadsheetReaderPort;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Importación de estudiantes desde Excel. Una fila incorrecta no aborta la importación: se valida todo y se aplican
 * (en una sola transacción) las filas válidas.
 */
public class StudentsImportProcessor implements ImportProcessor {

    private static final int MAX_ROWS = 5000;

    private final SpreadsheetReaderPort reader;
    private final StudentRepositoryPort students;
    private final GradeRepositoryPort grades;
    private final GroupRepositoryPort groups;
    private final StudentImportApplier applier;
    private final Clock clock;

    public StudentsImportProcessor(SpreadsheetReaderPort reader, StudentRepositoryPort students,
                                   GradeRepositoryPort grades, GroupRepositoryPort groups,
                                   StudentImportApplier applier, Clock clock) {
        this.reader = reader;
        this.students = students;
        this.grades = grades;
        this.groups = groups;
        this.applier = applier;
        this.clock = clock;
    }

    @Override
    public ImportType type() {
        return ImportType.STUDENTS;
    }

    @Override
    public ImportOutcome process(ImportBatch batch, byte[] content, ImportProgress progress) {
        Long teacherId = batch.getUserId();
        ParsedSheet sheet = reader.read(content).canonicalize(SHEET);
        requireColumns(sheet);
        if (sheet.rows().size() > MAX_ROWS) {
            throw new InvalidRequestException("TOO_MANY_ROWS", ImportMessages.tooManyRows(MAX_ROWS));
        }
        progress.start(sheet.rows().size());
        progress.step(SHEET.label());
        RowErrors errors = new RowErrors();
        List<StudentImportRow> valid = validate(teacherId, sheet.rows(), errors, progress);
        applier.apply(teacherId, valid);
        // una fila puede tener varios errores: cuenta una vez
        int failedRows = (int) errors.list().stream().map(ImportRowError::rowNumber).distinct().count();
        return new ImportOutcome(sheet.rows().size(), valid.size(), failedRows, errors.list());
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

    private List<StudentImportRow> validate(Long teacherId, List<SpreadsheetRow> rows, RowErrors errors,
                                            ImportProgress progress) {
        List<StudentImportRow> valid = new ArrayList<>();
        Map<String, Integer> seenIdentifications = new HashMap<>();
        Map<String, Integer> seenCodes = new HashMap<>();
        Map<String, Optional<Long>> groupCache = new HashMap<>();
        int currentYear = LocalDateTime.now(clock).getYear();

        for (SpreadsheetRow sheetRow : rows) {
            progress.tick();
            RowReader row = RowReader.headerOnly(sheetRow, SHEET, errors);
            int before = errors.size();
            String identification = row.required("identification_number", 50);
            String firstName = row.required("first_name", 100);
            String lastName = row.required("last_name", 100);
            String gradeName = row.required("grade", 50);
            String groupName = row.required("group", 50);
            String email = row.optionalEmail("email");
            String code = row.raw("student_code");
            if (code != null && code.length() > 50) {
                row.error("student_code", ImportMessages.maxLength(50));
            }
            int year = row.optionalYear("academic_year", currentYear);
            if (identification != null) {
                Integer first = seenIdentifications.putIfAbsent(identification, row.rowNumber());
                if (first != null) {
                    row.error("identification_number", ImportMessages.duplicatedInFile(first));
                }
            }
            if (code != null) {
                Integer first = seenCodes.putIfAbsent(code, row.rowNumber());
                if (first != null) {
                    row.error("student_code", ImportMessages.duplicatedInFile(first));
                }
            }
            Long groupId = null;
            if (gradeName != null && groupName != null && errors.size() == before) {
                groupId = resolveGroup(row, gradeName, groupName, year, groupCache, teacherId);
            }
            if (errors.size() > before) {
                continue;
            }
            resolveStudent(row, teacherId, identification, code, firstName, lastName, email, groupId)
                    .ifPresent(valid::add);
        }
        return valid;
    }

    private Long resolveGroup(RowReader row, String gradeName, String groupName, int year,
                              Map<String, Optional<Long>> cache, Long teacherId) {
        String key = gradeName + "|" + groupName + "|" + year;
        Optional<Long> groupId = cache.computeIfAbsent(key,
                k -> groups.findByGradeNameAndNameAndAcademicYear(teacherId, gradeName, groupName, year).map(Group::getId));
        if (groupId.isEmpty()) {
            boolean gradeExists = grades.findByTeacherIdAndName(teacherId, gradeName).isPresent();
            row.error(gradeExists ? "group" : "grade", gradeExists
                    ? "El grupo '" + groupName + "' no existe en el grado '" + gradeName + "' para " + year
                    : "El grado '" + gradeName + "' no existe");
            return null;
        }
        return groupId.get();
    }

    /** Decide crear o reutilizar (y actualizar) a un estudiante del propio profesor. */
    private Optional<StudentImportRow> resolveStudent(RowReader row, Long teacherId, String identification,
                                                      String code, String firstName, String lastName, String email,
                                                      Long groupId) {
        Optional<Student> byIdentification = students.findByTeacherIdAndIdentificationNumber(teacherId, identification);
        Optional<Student> byCode = code == null ? Optional.empty() : students.findByTeacherIdAndStudentCode(teacherId, code);
        if (byIdentification.isPresent() && byCode.isPresent() && !byIdentification.get().getId().equals(byCode.get().getId())) {
            row.error("student_code", "El código pertenece a un estudiante distinto al del número de identificación");
            return Optional.empty();
        }
        if (byIdentification.isEmpty() && byCode.isPresent()) {
            row.error("student_code", "El código ya lo usa otro estudiante");
            return Optional.empty();
        }
        Long existingId = byIdentification.map(Student::getId).orElse(null);
        return Optional.of(new StudentImportRow(row.rowNumber(), existingId, identification, code, firstName,
                lastName, email, groupId));
    }
}
