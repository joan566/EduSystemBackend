package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.entity.Grade;
import com.edusistem.core.academic.domain.inputports.ManageGroupUseCase;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.util.List;

/** Hoja Grupos: crea los grupos que no existan (grado + nombre + año lectivo); el grado debe existir. */
public class GroupsSheetImporter extends SheetImporterBase {

    private final GroupRepositoryPort groups;
    private final GradeRepositoryPort grades;
    private final ManageGroupUseCase manageGroup;

    public GroupsSheetImporter(GroupRepositoryPort groups, GradeRepositoryPort grades, ManageGroupUseCase manageGroup) {
        super(SchoolSetupSheets.GROUPS, List.of("grade_name", "name", "academic_year"));
        this.groups = groups;
        this.grades = grades;
        this.manageGroup = manageGroup;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            String gradeName = row.required("grade_name", 50);
            String name = row.required("name", 50);
            Integer year = row.requiredYear("academic_year");
            if (context.errors().size() > before) {
                markFailed(context, row);
                continue;
            }
            try {
                if (groups.findByGradeNameAndNameAndAcademicYear(teacherId, gradeName, name, year).isEmpty()) {
                    Long gradeId = grades.findByTeacherIdAndName(teacherId, gradeName).map(Grade::getId).orElse(null);
                    if (gradeId == null) {
                        row.error("grade_name",
                                ImportMessages.gradeNotFound(gradeName, label(SchoolSetupSheets.ACADEMIC_GRADES)));
                        markFailed(context, row);
                        continue;
                    }
                    manageGroup.create(new AcademicCommands.CreateGroup(teacherId, gradeId, name, year));
                }
            } catch (RuntimeException e) {
                fail(context, row, ImportMessages.couldNotCreate(e.getMessage()));
            }
        }
    }
}
