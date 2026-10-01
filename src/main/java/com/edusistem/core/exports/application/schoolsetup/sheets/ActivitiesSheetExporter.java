package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.ArrayList;
import java.util.List;

public class ActivitiesSheetExporter extends SheetExporterBase {

    public ActivitiesSheetExporter() {
        super(SchoolSetupSheets.ACTIVITIES);
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        List<List<Object>> rows = new ArrayList<>();
        for (TeachingPeriodView tp : context.classes()) {
            for (ActivityView a : context.activities(tp)) {
                rows.add(row(tp, a.name(), a.description(), a.evaluationDate(), a.maximumScore(), a.activityType()));
            }
        }
        return table(rows);
    }
}
