package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageGradeUseCase;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.util.List;

/** Hoja Grados: crea los grados que no existan (por nombre). */
public class AcademicGradesSheetImporter extends SheetImporterBase {

    private final GradeRepositoryPort grades;
    private final ManageGradeUseCase manageGrade;

    public AcademicGradesSheetImporter(GradeRepositoryPort grades, ManageGradeUseCase manageGrade) {
        super(SchoolSetupSheets.ACADEMIC_GRADES, List.of("name"));
        this.grades = grades;
        this.manageGrade = manageGrade;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            String name = row.required("name", 50);
            String description = row.optional("description", 255);
            if (context.errors().size() > before) {
                markFailed(context, row);
                continue;
            }
            try {
                if (grades.findByTeacherIdAndName(teacherId, name).isEmpty()) {
                    manageGrade.create(new AcademicCommands.CreateGrade(teacherId, name, description));
                }
            } catch (RuntimeException e) {
                fail(context, row, ImportMessages.couldNotCreate(e.getMessage()));
            }
        }
    }
}
