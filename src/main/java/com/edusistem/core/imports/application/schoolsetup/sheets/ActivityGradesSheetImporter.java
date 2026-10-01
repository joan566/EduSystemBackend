package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.imports.application.contracts.ClassResolver;
import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.imports.application.contracts.TeachingPeriodCatalog;
import com.edusistem.core.imports.application.schoolsetup.ClassKey;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext.ActivityRef;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoja Notas de actividades: nota de cada estudiante en cada actividad. Se registran por actividad a través del mismo
 * caso de uso que el endpoint de notas (0 ≤ nota ≤ puntaje máximo, estudiante matriculado).
 */
public class ActivityGradesSheetImporter extends ClassBoundSheetImporter {

    private static final Logger log = LoggerFactory.getLogger(ActivityGradesSheetImporter.class);

    private final TeachingPeriodCatalog catalog;
    private final StudentRoster roster;
    private final GradeActivityUseCase gradeActivity;

    public ActivityGradesSheetImporter(ClassResolver classes, TeachingPeriodCatalog catalog, StudentRoster roster,
                                       GradeActivityUseCase gradeActivity) {
        super(SchoolSetupSheets.ACTIVITY_GRADES, withClassKey("activity_name", "identification_number", "grade"),
                classes);
        this.catalog = catalog;
        this.roster = roster;
        this.gradeActivity = gradeActivity;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Map<Long, List<ActivityCommands.GradeInput>> inputsByActivity = new HashMap<>();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            ClassKey key = ClassKey.read(row);
            String activityName = row.required("activity_name", 150);
            String identification = row.required("identification_number", 50);
            String rawGrade = row.required("grade", 20);
            if (context.errors().size() > before || key == null) {
                markFailed(context, row);
                continue;
            }
            Long teachingPeriodId = teachingPeriod(context, row, key);
            if (teachingPeriodId == null) {
                markFailed(context, row);
                continue;
            }
            ActivityRef activity = context.activities(teachingPeriodId, catalog).get(activityName);
            if (activity == null) {
                row.error("activity_name",
                        ImportMessages.activityNotFound(activityName, label(SchoolSetupSheets.ACTIVITIES)));
                markFailed(context, row);
                continue;
            }
            Long studentId = enrolledStudent(context, row, teachingPeriodId, identification, roster);
            BigDecimal grade = studentId == null ? null : grade(row, rawGrade, activity.maximumScore());
            if (grade == null) {
                markFailed(context, row);
                continue;
            }
            inputsByActivity.computeIfAbsent(activity.activityId(), k -> new ArrayList<>())
                    .add(new ActivityCommands.GradeInput(studentId, grade, null));
        }
        inputsByActivity.forEach((activityId, inputs) -> {
            try {
                gradeActivity.recordGrades(new ActivityCommands.RecordGrades(context.teacherId(), activityId, inputs));
            } catch (RuntimeException e) {
                log.warn("Could not save grades for activity {}: {}", activityId, e.getMessage());
                context.errors().add(0, spec().label(), ImportMessages.couldNotSave(e.getMessage()));
            }
        });
    }

    /** Nota entre 0 y el puntaje máximo de la actividad; null (con el error registrado) si no. */
    private static BigDecimal grade(RowReader row, String rawGrade, BigDecimal maximumScore) {
        BigDecimal grade;
        try {
            grade = new BigDecimal(rawGrade);
        } catch (NumberFormatException e) {
            row.error("grade", ImportMessages.NOT_A_NUMBER);
            return null;
        }
        if (grade.signum() < 0 || grade.compareTo(maximumScore) > 0) {
            row.error("grade", ImportMessages.between(maximumScore));
            return null;
        }
        return grade;
    }
}
