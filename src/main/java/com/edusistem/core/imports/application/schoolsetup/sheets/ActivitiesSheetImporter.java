package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.inputports.ManageActivityUseCase;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.imports.application.contracts.ClassResolver;
import com.edusistem.core.imports.application.contracts.TeachingPeriodCatalog;
import com.edusistem.core.imports.application.schoolsetup.ClassKey;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext.ActivityRef;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Hoja Actividades: crea las actividades que la clase aún no tenga (por nombre). */
public class ActivitiesSheetImporter extends ClassBoundSheetImporter {

    private final TeachingPeriodCatalog catalog;
    private final ManageActivityUseCase manageActivity;

    public ActivitiesSheetImporter(ClassResolver classes, TeachingPeriodCatalog catalog,
                                   ManageActivityUseCase manageActivity) {
        super(SchoolSetupSheets.ACTIVITIES, withClassKey("name", "maximum_score"), classes);
        this.catalog = catalog;
        this.manageActivity = manageActivity;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            ClassKey key = ClassKey.read(row);
            String name = row.required("name", 150);
            String activityType = row.optional("activity_type", 50);
            BigDecimal maximumScore = row.requiredPositiveDecimal("maximum_score");
            LocalDateTime evaluationDate = row.optionalDateTime("evaluation_date");
            String description = row.raw("description");
            if (context.errors().size() > before || key == null) {
                markFailed(context, row);
                continue;
            }
            Long teachingPeriodId = teachingPeriod(context, row, key);
            if (teachingPeriodId == null) {
                markFailed(context, row);
                continue;
            }
            Map<String, ActivityRef> existing = context.activities(teachingPeriodId, catalog);
            if (existing.containsKey(name)) {
                continue;
            }
            try {
                ActivityView created = manageActivity.create(new ActivityCommands.Create(teacherId, teachingPeriodId,
                        name, description, evaluationDate, maximumScore, activityType));
                existing.put(name, new ActivityRef(created.activityId(), created.maximumScore()));
            } catch (RuntimeException e) {
                fail(context, row, ImportMessages.couldNotCreate(e.getMessage()));
            }
        }
    }
}
