package com.edusistem.core.exports.application.schoolsetup.sheets;

import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.attendance.domain.entity.AttendanceRecord;
import com.edusistem.core.attendance.domain.outputports.AttendanceRecordRepositoryPort;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import com.edusistem.core.shared.domain.vo.TabularData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Hoja Asistencia: una fila por estudiante y sesión, sesiones en orden de fecha. */
public class AttendanceSheetExporter extends SheetExporterBase {

    private final AttendanceSessionRepositoryPort sessions;
    private final AttendanceRecordRepositoryPort records;

    public AttendanceSheetExporter(AttendanceSessionRepositoryPort sessions, AttendanceRecordRepositoryPort records) {
        super(SchoolSetupSheets.ATTENDANCE);
        this.sessions = sessions;
        this.records = records;
    }

    @Override
    public TabularData export(SchoolSetupExportContext context) {
        List<List<Object>> rows = new ArrayList<>();
        for (TeachingPeriodView tp : context.classes()) {
            Map<Long, String> identificationById = context.identificationById(tp);
            List<AttendanceSessionView> sessionList = PageResult.collectAll(
                    page -> sessions.findViewsByTeachingPeriodId(tp.id(), page));
            sessionList.sort(Comparator.comparing(AttendanceSessionView::sessionDate));
            for (AttendanceSessionView session : sessionList) {
                for (AttendanceRecord r : records.findBySessionId(session.sessionId())) {
                    String identification = identificationById.get(r.getStudentId());
                    if (identification != null && r.getStatus() != null) {
                        rows.add(row(tp, session.sessionDate(), identification,
                                SpreadsheetVocabulary.attendanceLabel(r.getStatus().name())));
                    }
                }
            }
        }
        return table(rows);
    }
}
