package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;

public class AcademicGradesSheetExporter extends SheetExporterBase {

    private final GradeRepositoryPort grades;

    public AcademicGradesSheetExporter(GradeRepositoryPort grades) {
        super(SchoolSetupSheets.ACADEMIC_GRADES);
        this.grades = grades;
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        return table(grades.findByTeacherIdOrderedByName(context.teacherId()).stream()
                .map(g -> values(g.getName(), g.getDescription())).toList());
    }
}
