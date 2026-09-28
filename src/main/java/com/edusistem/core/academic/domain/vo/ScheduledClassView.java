package com.edusistem.core.academic.domain.vo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** Bloque de horario con el contexto (asignatura, grupo, periodo) necesario para mostrarlo en la agenda. */
public record ScheduledClassView(Long scheduleId, Long teachingPeriodId, DayOfWeek dayOfWeek, LocalTime startTime,
                                 LocalTime endTime, String room, Long subjectId, String subjectName, Long groupId,
                                 String gradeName, String groupName, Long academicPeriodId, String academicPeriodName,
                                 LocalDate periodStartDate, LocalDate periodEndDate) {

    public boolean coversDate(LocalDate date) {
        return !date.isBefore(periodStartDate) && !date.isAfter(periodEndDate);
    }
}
