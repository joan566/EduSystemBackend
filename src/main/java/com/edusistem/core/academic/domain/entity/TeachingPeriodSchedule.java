package com.edusistem.core.academic.domain.entity;

import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bloque semanal de clase de un periodo de enseñanza (p. ej. lunes 07:00-08:00). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingPeriodSchedule {
    private Long id;
    private Long teachingPeriodId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Normaliza las horas a minutos y exige que la clase termine después de empezar. */
    public void normalizeAndValidate() {
        startTime = startTime.truncatedTo(ChronoUnit.MINUTES);
        endTime = endTime.truncatedTo(ChronoUnit.MINUTES);
        if (!endTime.isAfter(startTime)) {
            throw new InvalidRequestException("INVALID_SCHEDULE_TIMES", "endTime must be after startTime");
        }
        if (room != null) {
            room = room.isBlank() ? null : room.strip();
        }
    }
}
