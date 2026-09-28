package com.edusistem.core.academic.domain.inputports;

import com.edusistem.core.academic.domain.vo.ScheduleCalendar;
import java.time.LocalDate;

public interface QueryTeacherScheduleUseCase {

    /** Clases de un día; si {@code date} es null usa la fecha actual del colegio. */
    ScheduleCalendar day(Long teacherId, LocalDate date);

    /** Clases día por día entre {@code from} y {@code to} (inclusive); por defecto la semana que empieza hoy. */
    ScheduleCalendar range(Long teacherId, LocalDate from, LocalDate to);
}
