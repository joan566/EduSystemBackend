package com.edusistem.core.academic.application.use_case.service;

import com.edusistem.core.academic.domain.inputports.QueryTeacherScheduleUseCase;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.academic.domain.vo.ScheduleCalendar;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Agenda del profesor calculada en la zona horaria del colegio, no en la del servidor ni la del cliente. */
public class TeacherScheduleService implements QueryTeacherScheduleUseCase {

    static final int MAX_RANGE_DAYS = 93;
    private static final int DEFAULT_RANGE_DAYS = 7;

    private final TeachingPeriodScheduleRepositoryPort schedules;
    private final Clock clock;
    private final ZoneId schoolZone;

    public TeacherScheduleService(TeachingPeriodScheduleRepositoryPort schedules, Clock clock, ZoneId schoolZone) {
        this.schedules = schedules;
        this.clock = clock;
        this.schoolZone = schoolZone;
    }

    @Override
    public ScheduleCalendar day(Long teacherId, LocalDate date) {
        LocalDateTime now = now();
        LocalDate day = date != null ? date : now.toLocalDate();
        return calendar(teacherId, now, day, day);
    }

    @Override
    public ScheduleCalendar range(Long teacherId, LocalDate from, LocalDate to) {
        LocalDateTime now = now();
        LocalDate start = from != null ? from : now.toLocalDate();
        LocalDate end = to != null ? to : start.plusDays(DEFAULT_RANGE_DAYS - 1);
        if (end.isBefore(start)) {
            throw new InvalidRequestException("INVALID_DATE_RANGE", "to must not be before from");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new InvalidRequestException("INVALID_DATE_RANGE",
                    "The range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        return calendar(teacherId, now, start, end);
    }

    private ScheduleCalendar calendar(Long teacherId, LocalDateTime now, LocalDate from, LocalDate to) {
        List<ScheduledClassView> candidates = schedules.findViewsByTeacher(teacherId, from, to).stream()
                .sorted(TeachingPeriodScheduleService.BY_DAY_AND_TIME).toList();
        List<ScheduleCalendar.Day> days = from.datesUntil(to.plusDays(1))
                .map(date -> new ScheduleCalendar.Day(date, candidates.stream()
                        .filter(c -> c.dayOfWeek() == date.getDayOfWeek() && c.coversDate(date)).toList()))
                .toList();
        return new ScheduleCalendar(now, schoolZone, days);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock.withZone(schoolZone)).truncatedTo(ChronoUnit.SECONDS);
    }
}
