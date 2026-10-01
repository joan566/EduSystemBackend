package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageAcademicPeriodUseCase;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.time.LocalDate;
import java.util.List;

/** Hoja Periodos: crea los periodos académicos que no existan (por nombre). */
public class AcademicPeriodsSheetImporter extends SheetImporterBase {

    private final AcademicPeriodRepositoryPort academicPeriods;
    private final ManageAcademicPeriodUseCase manageAcademicPeriod;

    public AcademicPeriodsSheetImporter(AcademicPeriodRepositoryPort academicPeriods,
                                        ManageAcademicPeriodUseCase manageAcademicPeriod) {
        super(SchoolSetupSheets.ACADEMIC_PERIODS, List.of("name", "start_date", "end_date"));
        this.academicPeriods = academicPeriods;
        this.manageAcademicPeriod = manageAcademicPeriod;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            String name = row.required("name", 100);
            LocalDate startDate = row.requiredDate("start_date");
            LocalDate endDate = row.requiredDate("end_date");
            if (context.errors().size() > before) {
                markFailed(context, row);
                continue;
            }
            try {
                if (academicPeriods.findByTeacherIdAndName(teacherId, name).isEmpty()) {
                    manageAcademicPeriod.create(new AcademicCommands.SavePeriod(teacherId, null, name, startDate, endDate));
                }
            } catch (RuntimeException e) {
                fail(context, row, ImportMessages.couldNotCreate(e.getMessage()));
            }
        }
    }
}
