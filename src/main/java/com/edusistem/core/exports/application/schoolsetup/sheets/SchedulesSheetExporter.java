package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SchedulesSheetExporter extends SheetExporterBase {

    private final TeachingPeriodScheduleRepositoryPort schedules;

    public SchedulesSheetExporter(TeachingPeriodScheduleRepositoryPort schedules) {
        super(SchoolSetupSheets.SCHEDULES);
        this.schedules = schedules;
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        List<List<Object>> rows = new ArrayList<>();
        for (TeachingPeriodView tp : context.classes()) {
            schedules.findViewsByTeachingPeriodId(tp.id()).stream()
                    .sorted(Comparator.comparing(ScheduledClassView::dayOfWeek).thenComparing(ScheduledClassView::startTime))
                    .forEach(s -> rows.add(row(tp, SpreadsheetVocabulary.dayLabel(s.dayOfWeek()), s.startTime(),
                            s.endTime(), s.room())));
        }
        return table(rows);
    }
}
