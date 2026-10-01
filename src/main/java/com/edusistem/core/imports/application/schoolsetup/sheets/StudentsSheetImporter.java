package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.student.StudentImportApplier;
import com.edusistem.core.imports.application.student.StudentImportRow;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
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
 * Hoja Estudiantes: crea o actualiza estudiantes y los matricula en su grupo. El mismo estudiante puede venir en varias
 * filas si está matriculado en varios grupos. Las filas válidas se aplican juntas en una transacción.
 */
public class StudentsSheetImporter extends SheetImporterBase {

    private final StudentRepositoryPort students;
    private final GroupRepositoryPort groups;
    private final StudentImportApplier applier;
    private final Clock clock;

    public StudentsSheetImporter(StudentRepositoryPort students, GroupRepositoryPort groups,
                                 StudentImportApplier applier, Clock clock) {
        super(SchoolSetupSheets.STUDENTS,
                List.of("identification_number", "first_name", "last_name", "grade_name", "group_name"));
        this.students = students;
        this.groups = groups;
        this.applier = applier;
        this.clock = clock;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        List<StudentImportRow> valid = new ArrayList<>();
        Map<String, Integer> seenEnrollments = new HashMap<>();
        Map<String, Optional<Long>> groupCache = new HashMap<>();
        int currentYear = LocalDateTime.now(clock).getYear();

        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            String identification = row.required("identification_number", 50);
            String firstName = row.required("first_name", 100);
            String lastName = row.required("last_name", 100);
            String gradeName = row.required("grade_name", 50);
            String groupName = row.required("group_name", 50);
            String email = row.optionalEmail("email");
            int year = row.optionalYear("academic_year", currentYear);
            if (identification != null) {
                String enrollmentKey = identification + "|" + gradeName + "|" + groupName + "|" + year;
                Integer first = seenEnrollments.putIfAbsent(enrollmentKey, row.rowNumber());
                if (first != null) {
                    row.error("identification_number", ImportMessages.duplicatedInFile(first));
                }
            }
            Long groupId = null;
            if (gradeName != null && groupName != null && context.errors().size() == before) {
                groupId = enrollmentGroup(teacherId, row, gradeName, groupName, year, groupCache);
            }
            if (context.errors().size() > before) {
                markFailed(context, row);
                continue;
            }
            Long existingId = students.findByTeacherIdAndIdentificationNumber(teacherId, identification)
                    .map(Student::getId).orElse(null);
            valid.add(new StudentImportRow(row.rowNumber(), existingId, identification, null, firstName, lastName,
                    email, groupId));
        }
        applier.apply(teacherId, valid);
    }

    private Long enrollmentGroup(Long teacherId, RowReader row, String gradeName, String groupName, int year,
                                 Map<String, Optional<Long>> cache) {
        Optional<Long> groupId = cache.computeIfAbsent(gradeName + "|" + groupName + "|" + year,
                k -> groups.findByGradeNameAndNameAndAcademicYear(teacherId, gradeName, groupName, year).map(Group::getId));
        if (groupId.isEmpty()) {
            row.error("group_name",
                    ImportMessages.groupNotFound(groupName, gradeName, year, label(SchoolSetupSheets.GROUPS)));
            return null;
        }
        return groupId.get();
    }
}
