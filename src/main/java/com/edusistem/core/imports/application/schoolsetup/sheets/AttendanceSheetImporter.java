package com.edusistem.core.imports.application.schoolsetup.sheets;

import com.edusistem.core.attendance.application.use_case.dtos.AttendanceCommands;
import com.edusistem.core.attendance.domain.enums.AttendanceStatus;
import com.edusistem.core.attendance.domain.inputports.ManageAttendanceUseCase;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.imports.application.contracts.ClassResolver;
import com.edusistem.core.imports.application.contracts.StudentRoster;
import com.edusistem.core.imports.application.contracts.TeachingPeriodCatalog;
import com.edusistem.core.imports.application.schoolsetup.ClassKey;
import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SchoolSetupSheets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hoja Asistencia: estado de cada estudiante por fecha. Crea la sesión de asistencia de la fecha si no existe y
 * registra la asistencia por sesión a través del mismo caso de uso que el endpoint de asistencia.
 */
public class AttendanceSheetImporter extends ClassBoundSheetImporter {

    private static final Logger log = LoggerFactory.getLogger(AttendanceSheetImporter.class);

    private final TeachingPeriodCatalog catalog;
    private final StudentRoster roster;
    private final ManageAttendanceUseCase attendance;

    public AttendanceSheetImporter(ClassResolver classes, TeachingPeriodCatalog catalog, StudentRoster roster,
                                   ManageAttendanceUseCase attendance) {
        super(SchoolSetupSheets.ATTENDANCE, withClassKey("session_date", "identification_number", "status"), classes);
        this.catalog = catalog;
        this.roster = roster;
        this.attendance = attendance;
    }

    @Override
    protected void importRows(SchoolSetupImportContext context, List<SpreadsheetRow> rows) {
        Long teacherId = context.teacherId();
        Map<Long, List<AttendanceCommands.RecordInput>> inputsBySession = new HashMap<>();
        for (SpreadsheetRow sheetRow : rows) {
            RowReader row = reader(context, sheetRow);
            int before = context.errors().size();
            ClassKey key = ClassKey.read(row);
            LocalDate sessionDate = row.requiredDate("session_date");
            String identification = row.required("identification_number", 50);
            AttendanceStatus status = row.requiredAttendanceStatus("status");
            if (context.errors().size() > before || key == null) {
                markFailed(context, row);
                continue;
            }
            Long teachingPeriodId = teachingPeriod(context, row, key);
            if (teachingPeriodId == null) {
                markFailed(context, row);
                continue;
            }
            Long studentId = enrolledStudent(context, row, teachingPeriodId, identification, roster);
            if (studentId == null) {
                markFailed(context, row);
                continue;
            }
            Long sessionId = sessionFor(context, teachingPeriodId, sessionDate);
            if (sessionId == null) {
                row.error("session_date", ImportMessages.SESSION_NOT_CREATED);
                markFailed(context, row);
                continue;
            }
            inputsBySession.computeIfAbsent(sessionId, k -> new ArrayList<>())
                    .add(new AttendanceCommands.RecordInput(studentId, status, null));
        }
        inputsBySession.forEach((sessionId, inputs) -> {
            try {
                attendance.recordAttendance(new AttendanceCommands.RecordAttendance(teacherId, sessionId, inputs));
            } catch (RuntimeException e) {
                log.warn("Could not save attendance for session {}: {}", sessionId, e.getMessage());
                context.errors().add(0, spec().label(), ImportMessages.couldNotSave(e.getMessage()));
            }
        });
    }

    /** Sesión de la fecha (la crea si no existe); null si no se pudo crear. */
    private Long sessionFor(SchoolSetupImportContext context, Long teachingPeriodId, LocalDate sessionDate) {
        Map<LocalDate, Long> byDate = context.sessions(teachingPeriodId, catalog);
        Long sessionId = byDate.get(sessionDate);
        if (sessionId != null) {
            return sessionId;
        }
        try {
            AttendanceSessionView created = attendance.createSession(
                    new AttendanceCommands.CreateSession(context.teacherId(), teachingPeriodId, sessionDate, null, null));
            byDate.put(sessionDate, created.sessionId());
            return created.sessionId();
        } catch (RuntimeException e) {
            log.warn("Could not create attendance session for teaching period {} on {}: {}", teachingPeriodId,
                    sessionDate, e.getMessage());
            return null;
        }
    }
}
