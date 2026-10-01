package com.edusistem.core.imports.application.teachingperiod.sheets;

import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.imports.application.contracts.TeachingPeriodCatalog;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.application.teachingperiod.TeachingPeriodImportContext;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hoja Notas: una columna por actividad (id en el encabezado), una fila por estudiante. */
public class PeriodGradesSheetImporter extends PeriodSheetImporterBase {

    private static final Logger log = LoggerFactory.getLogger(PeriodGradesSheetImporter.class);

    private final TeachingPeriodCatalog catalog;
    private final GradeActivityUseCase gradeActivity;

    public PeriodGradesSheetImporter(TeachingPeriodCatalog catalog, GradeActivityUseCase gradeActivity) {
        super(SpreadsheetVocabulary.GRADES, List.of("identification_number"));
        this.catalog = catalog;
        this.gradeActivity = gradeActivity;
    }

    @Override
    protected void importRows(TeachingPeriodImportContext context, ParsedSheet sheet) {
        Map<Long, ActivityView> activityById = catalog.activities(context.teachingPeriodId()).stream()
                .collect(Collectors.toMap(ActivityView::activityId, Function.identity()));
        Map<String, ActivityView> activityByHeader = columnsById(sheet, activityById);

        Map<Long, List<ActivityCommands.GradeInput>> inputsByActivity = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (SpreadsheetRow sheetRow : sheet.rows()) {
            RowReader row = reader(context, sheetRow);
            Long studentId = rosterStudent(context, row, seen);
            if (studentId == null) {
                continue;
            }
            boolean rowFailed = false;
            for (Map.Entry<String, ActivityView> entry : activityByHeader.entrySet()) {
                String raw = row.raw(entry.getKey());
                if (raw == null) {
                    continue;
                }
                ActivityView activity = entry.getValue();
                BigDecimal grade;
                try {
                    grade = new BigDecimal(raw);
                } catch (NumberFormatException e) {
                    row.error(activity.name(), ImportMessages.NOT_A_NUMBER);
                    rowFailed = true;
                    continue;
                }
                if (grade.signum() < 0 || grade.compareTo(activity.maximumScore()) > 0) {
                    row.error(activity.name(), ImportMessages.between(activity.maximumScore()));
                    rowFailed = true;
                    continue;
                }
                inputsByActivity.computeIfAbsent(activity.activityId(), k -> new ArrayList<>())
                        .add(new ActivityCommands.GradeInput(studentId, grade, null));
            }
            if (rowFailed) {
                markFailed(context, row);
            }
        }
        inputsByActivity.forEach((activityId, inputs) -> {
            try {
                gradeActivity.recordGrades(new ActivityCommands.RecordGrades(context.teacherId(), activityId, inputs));
            } catch (RuntimeException e) {
                log.warn("Could not save grades for activity {}: {}", activityId, e.getMessage());
                context.errors().add(0, spec().ref(activityById.get(activityId).name()),
                        ImportMessages.couldNotSave(e.getMessage()));
            }
        });
    }
}
