package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.subject.application.use_case.dtos.SubjectCommands;
import com.edusistem.core.subject.domain.inputports.ManageSubjectUseCase;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import java.util.List;

/** Hoja Asignaturas: crea las asignaturas que no existan (por nombre). */
public class SubjectsSheetImporter extends SheetImporterBase {

    private final SubjectRepositoryPort subjects;
    private final ManageSubjectUseCase manageSubject;

    public SubjectsSheetImporter(SubjectRepositoryPort subjects, ManageSubjectUseCase manageSubject) {
        super(SchoolSetupSheets.SUBJECTS, List.of("name"));
        this.subjects = subjects;
        this.manageSubject = manageSubject;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            String name = row.required("name", 100);
            String description = row.optional("description", 255);
            if (context.errors().size() > before) {
                markFailed(context, row);
                continue;
            }
            try {
                if (subjects.findByTeacherIdAndName(teacherId, name).isEmpty()) {
                    manageSubject.create(new SubjectCommands.Create(teacherId, name, description));
                }
            } catch (RuntimeException e) {
                fail(context, row, ImportMessages.couldNotCreate(e.getMessage()));
            }
        }
    }
}
