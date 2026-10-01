package com.edusistem.core.imports.application.classsetup;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.inputports.ManageTeachingPeriodScheduleUseCase;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.imports.application.contracts.ClassScheduleConfigurer;
import com.edusistem.core.imports.application.support.CellValues;
import com.edusistem.core.imports.application.support.ImportMessages;
import com.edusistem.core.imports.application.support.RowReader;
import com.edusistem.core.shared.domain.exceptions.DomainException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Bloques de horario de una clase que llegan en la hoja Horarios del Excel de configuración escolar. Un bloque con el
 * mismo día y horas ya existente no se duplica (sólo se actualiza el salón si cambió).
 */
public class ScheduleSetupApplier implements ClassScheduleConfigurer {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final ManageTeachingPeriodScheduleUseCase manageSchedule;
    private final TeachingPeriodScheduleRepositoryPort schedules;
    private final TeachingPeriodRepositoryPort teachingPeriods;

    public ScheduleSetupApplier(ManageTeachingPeriodScheduleUseCase manageSchedule,
                                TeachingPeriodScheduleRepositoryPort schedules,
                                TeachingPeriodRepositoryPort teachingPeriods) {
        this.manageSchedule = manageSchedule;
        this.schedules = schedules;
        this.teachingPeriods = teachingPeriods;
    }

    @Override
    public ScheduleInput read(RowReader row) {
        boolean valid = true;
        DayOfWeek day = null;
        String rawDay = row.raw("day_of_week");
        if (rawDay == null) {
            row.error("day_of_week", ImportMessages.REQUIRED);
            valid = false;
        } else {
            day = SpreadsheetVocabulary.dayOfWeek(rawDay).orElse(null);
            if (day == null) {
                row.error("day_of_week", ImportMessages.INVALID_DAY);
                valid = false;
            }
        }
        LocalTime start = readTime(row, "start_time");
        LocalTime end = readTime(row, "end_time");
        if (start == null || end == null) {
            valid = false;
        } else if (!end.isAfter(start)) {
            row.error("end_time", ImportMessages.END_BEFORE_START);
            valid = false;
        }
        String room = row.raw("room");
        if (room != null && room.length() > 50) {
            row.error("room", ImportMessages.maxLength(50));
            valid = false;
        }
        return valid ? new ScheduleInput(day, start, end, room) : null;
    }

    @Override
    public boolean apply(Long teacherId, RowReader row, Long teachingPeriodId, ScheduleInput input) {
        Optional<ScheduledClassView> same = schedules.findViewsByTeachingPeriodId(teachingPeriodId).stream()
                .filter(s -> s.dayOfWeek() == input.day() && s.startTime().equals(input.start())
                        && s.endTime().equals(input.end()))
                .findFirst();
        try {
            if (same.isPresent()) {
                if (input.room() != null && !input.room().equals(same.get().room())) {
                    manageSchedule.update(new AcademicCommands.SaveSchedule(teacherId, teachingPeriodId,
                            same.get().scheduleId(), input.day(), input.start(), input.end(), input.room()));
                }
                return true;
            }
            TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                    .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
            Optional<ScheduledClassView> conflict = schedules.findConflict(teacherId, input.day(), input.start(),
                    input.end(), period.startDate(), period.endDate(), null);
            if (conflict.isPresent()) {
                ScheduledClassView other = conflict.get();
                row.error("start_time", ImportMessages.scheduleConflict(
                        other.subjectName() + " " + other.gradeName() + " " + other.groupName(),
                        SpreadsheetVocabulary.dayLabel(other.dayOfWeek()), other.startTime().format(HH_MM),
                        other.endTime().format(HH_MM)));
                return false;
            }
            manageSchedule.create(new AcademicCommands.SaveSchedule(teacherId, teachingPeriodId, null, input.day(),
                    input.start(), input.end(), input.room()));
            return true;
        } catch (DomainException e) {
            row.sheetError(ImportMessages.couldNotCreate(e.getMessage()));
            return false;
        }
    }

    private static LocalTime readTime(RowReader row, String column) {
        String raw = row.raw(column);
        if (raw == null) {
            row.error(column, ImportMessages.REQUIRED);
            return null;
        }
        LocalTime time = CellValues.time(raw);
        if (time == null) {
            row.error(column, ImportMessages.INVALID_TIME);
        }
        return time;
    }
}
