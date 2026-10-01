package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.activity.domain.inputports.GradeActivityUseCase;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.activity.domain.vo.StudentGradeView;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Hoja Notas de actividades: solo notas registradas de estudiantes con número de identificación. */
public class ActivityGradesSheetExporter extends SheetExporterBase {

    private final GradeActivityUseCase gradeActivity;

    public ActivityGradesSheetExporter(GradeActivityUseCase gradeActivity) {
        super(SchoolSetupSheets.ACTIVITY_GRADES);
        this.gradeActivity = gradeActivity;
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        List<List<Object>> rows = new ArrayList<>();
        for (TeachingPeriodView tp : context.classes()) {
            Map<Long, String> identificationById = context.identificationById(tp);
            for (ActivityView a : context.activities(tp)) {
                for (StudentGradeView g : gradeActivity.listGrades(context.teacherId(), a.activityId())) {
                    String identification = identificationById.get(g.studentId());
                    if (g.grade() != null && identification != null) {
                        rows.add(row(tp, a.name(), identification, g.grade()));
                    }
                }
            }
        }
        return table(rows);
    }
}
