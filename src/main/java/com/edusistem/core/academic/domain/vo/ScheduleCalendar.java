package com.edusistem.core.academic.domain.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** Clases del profesor por día, con la hora actual del colegio para que el cliente no dependa de su reloj. */
public record ScheduleCalendar(LocalDateTime serverTime, ZoneId timezone, List<Day> days) {

    public record Day(LocalDate date, List<ScheduledClassView> classes) {
    }
}
