package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import com.edusistem.core.student.domain.entity.Student;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Hoja Estudiantes: una fila por matrícula (estudiante + grupo). */
public class StudentsSheetExporter extends SheetExporterBase {

    public StudentsSheetExporter() {
        super(SchoolSetupSheets.STUDENTS);
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        List<List<Object>> rows = new ArrayList<>();
        for (GroupView group : context.groups()) {
            context.rosterByGroup().get(group.id()).stream()
                    .sorted(Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName))
                    .forEach(s -> rows.add(values(s.getIdentificationNumber(), s.getFirstName(), s.getLastName(),
                            s.getEmail(), group.gradeName(), group.name(), group.academicYear())));
        }
        return table(rows);
    }
}
