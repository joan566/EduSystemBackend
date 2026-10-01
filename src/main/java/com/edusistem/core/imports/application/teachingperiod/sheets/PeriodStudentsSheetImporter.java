package com.edusistem.core.imports.application.teachingperiod.sheets;

import com.edusistem.core.imports.application.student.StudentImportApplier;
import com.edusistem.core.imports.application.student.StudentImportRow;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.application.teachingperiod.TeachingPeriodImportContext;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Hoja Estudiantes: crea o actualiza estudiantes y los matricula en el grupo del teaching period. */
public class PeriodStudentsSheetImporter extends PeriodSheetImporterBase {

    private final StudentRepositoryPort students;
    private final StudentImportApplier applier;

    public PeriodStudentsSheetImporter(StudentRepositoryPort students, StudentImportApplier applier) {
        super(SpreadsheetVocabulary.STUDENTS, List.of("identification_number", "first_name", "last_name"));
        this.students = students;
        this.applier = applier;
    }

    @Override
    protected void importRows(TeachingPeriodImportContext context, ParsedSheet sheet) {
        List<StudentImportRow> valid = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (SpreadsheetRow sheetRow : sheet.rows()) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            String identification = row.required("identification_number", 50);
            String firstName = row.required("first_name", 100);
            String lastName = row.required("last_name", 100);
            String email = row.optionalEmail("email");
            if (identification != null) {
                Integer first = seen.putIfAbsent(identification, row.rowNumber());
                if (first != null) {
                    row.error("identification_number", ImportMessages.duplicatedInSheet(first));
                }
            }
            if (context.errors().size() > before) {
                markFailed(context, row);
                continue;
            }
            Long existingId = students.findByTeacherIdAndIdentificationNumber(context.teacherId(), identification)
                    .map(Student::getId).orElse(null);
            valid.add(new StudentImportRow(row.rowNumber(), existingId, identification, null, firstName, lastName,
                    email, context.groupId()));
        }
        applier.apply(context.teacherId(), valid);
    }
}
