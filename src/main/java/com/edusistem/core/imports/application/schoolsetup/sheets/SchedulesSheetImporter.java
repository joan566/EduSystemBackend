package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.imports.application.contracts.ClassResolver;
import com.edusistem.core.imports.application.contracts.ClassScheduleConfigurer;
import com.edusistem.core.imports.application.contracts.ClassScheduleConfigurer.ScheduleInput;
import com.edusistem.core.imports.application.schoolsetup.ClassKey;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.util.List;

/** Hoja Horarios: bloques semanales de cada clase. */
public class SchedulesSheetImporter extends ClassBoundSheetImporter {

    private final ClassScheduleConfigurer schedules;

    public SchedulesSheetImporter(ClassResolver classes, ClassScheduleConfigurer schedules) {
        super(SchoolSetupSheets.SCHEDULES, withClassKey("day_of_week", "start_time", "end_time"), classes);
        this.schedules = schedules;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            ClassKey key = ClassKey.read(row);
            ScheduleInput input = schedules.read(row);
            if (context.errors().size() > before || key == null) {
                markFailed(context, row);
                continue;
            }
            Long teachingPeriodId = teachingPeriod(context, row, key);
            if (teachingPeriodId == null || !schedules.apply(teacherId, row, teachingPeriodId, input)) {
                markFailed(context, row);
            }
        }
    }
}
