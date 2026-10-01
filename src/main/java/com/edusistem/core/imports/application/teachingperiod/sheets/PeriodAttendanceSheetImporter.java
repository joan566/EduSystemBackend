package com.edusistem.core.imports.application.teachingperiod.sheets;

import com.edusistem.core.attendance.application.use_case.dtos.AttendanceCommands;
import com.edusistem.core.attendance.domain.enums.AttendanceStatus;
import com.edusistem.core.attendance.domain.inputports.ManageAttendanceUseCase;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.imports.application.contracts.TeachingPeriodCatalog;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.imports.application.teachingperiod.TeachingPeriodImportContext;
import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hoja Asistencia: una columna por sesión (id en el encabezado), una fila por estudiante. */
public class PeriodAttendanceSheetImporter extends PeriodSheetImporterBase {

    private static final Logger log = LoggerFactory.getLogger(PeriodAttendanceSheetImporter.class);

    private final TeachingPeriodCatalog catalog;
    private final ManageAttendanceUseCase attendance;

    public PeriodAttendanceSheetImporter(TeachingPeriodCatalog catalog, ManageAttendanceUseCase attendance) {
        super(SpreadsheetVocabulary.ATTENDANCE, List.of("identification_number"));
        this.catalog = catalog;
        this.attendance = attendance;
    }

    @Override
    protected void importRows(TeachingPeriodImportContext context, ParsedSheet sheet) {
        Map<Long, AttendanceSessionView> sessionById = catalog.sessions(context.teachingPeriodId()).stream()
                .collect(Collectors.toMap(AttendanceSessionView::sessionId, Function.identity()));
        Map<String, AttendanceSessionView> sessionByHeader = columnsById(sheet, sessionById);

        Map<Long, List<AttendanceCommands.RecordInput>> inputsBySession = new HashMap<>();
        Set<String> seen = new HashSet<>();
        for (SpreadsheetRow sheetRow : sheet.rows()) {
            RowReader row = reader(context, sheetRow);
            Long studentId = rosterStudent(context, row, seen);
            if (studentId == null) {
                continue;
            }
            boolean rowFailed = false;
            for (Map.Entry<String, AttendanceSessionView> entry : sessionByHeader.entrySet()) {
                String raw = row.raw(entry.getKey());
                if (raw == null) {
                    continue;
                }
                AttendanceSessionView session = entry.getValue();
                Optional<AttendanceStatus> status = RowReader.attendanceStatus(raw);
                if (status.isEmpty()) {
                    row.error(session.sessionDate().toString(), ImportMessages.INVALID_ATTENDANCE);
                    rowFailed = true;
                    continue;
                }
                inputsBySession.computeIfAbsent(session.sessionId(), k -> new ArrayList<>())
                        .add(new AttendanceCommands.RecordInput(studentId, status.get(), null));
            }
            if (rowFailed) {
                markFailed(context, row);
            }
        }
        inputsBySession.forEach((sessionId, inputs) -> {
            try {
                attendance.recordAttendance(new AttendanceCommands.RecordAttendance(context.teacherId(), sessionId,
                        inputs));
            } catch (RuntimeException e) {
                log.warn("Could not save attendance for session {}: {}", sessionId, e.getMessage());
                context.errors().add(0, spec().ref(sessionById.get(sessionId).sessionDate().toString()),
                        ImportMessages.couldNotSave(e.getMessage()));
            }
        });
    }
}
