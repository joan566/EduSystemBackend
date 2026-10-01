package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.application.support.RowReader;
import java.time.DayOfWeek;
import java.time.LocalTime;

/** Bloques semanales de horario de una clase (hoja Horarios). */
public interface ClassScheduleConfigurer {

    record ScheduleInput(DayOfWeek day, LocalTime start, LocalTime end, String room) {
    }

    /** Lee día, horas y salón; null si alguna celda es inválida (los errores quedan registrados). */
    ScheduleInput read(RowReader row);

    /** Crea el bloque si no existe; devuelve false (con el error registrado) si no se pudo. */
    boolean apply(Long teacherId, RowReader row, Long teachingPeriodId, ScheduleInput input);
}
