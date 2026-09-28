package com.edusistem.core.academic.domain.outputports;

import com.edusistem.core.academic.domain.entity.TeachingPeriodSchedule;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TeachingPeriodScheduleRepositoryPort {

    TeachingPeriodSchedule save(TeachingPeriodSchedule schedule);

    Optional<TeachingPeriodSchedule> findById(Long id);

    Optional<ScheduledClassView> findViewById(Long id);

    List<ScheduledClassView> findViewsByTeachingPeriodId(Long teachingPeriodId);

    /**
     * Primer bloque de otra clase activa del profesor que se cruza con el horario dado el mismo día, dentro de
     * periodos académicos que se solapan con {@code [periodStart, periodEnd]}.
     */
    Optional<ScheduledClassView> findConflict(Long teacherId, DayOfWeek dayOfWeek, LocalTime startTime,
                                              LocalTime endTime, LocalDate periodStart, LocalDate periodEnd,
                                              Long excludeScheduleId);

    /** Bloques de las asignaciones activas del profesor cuyo periodo académico se solapa con {@code [from, to]}. */
    List<ScheduledClassView> findViewsByTeacher(Long teacherId, LocalDate from, LocalDate to);

    void deleteById(Long id);
}
