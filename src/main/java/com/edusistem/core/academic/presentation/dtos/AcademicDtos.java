package com.edusistem.core.academic.presentation.dtos;

import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.entity.Grade;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.academic.domain.vo.ScheduleCalendar;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.academic.domain.vo.TeachingAssignmentView;
import com.edusistem.core.academic.domain.vo.TeachingPeriodSummary;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class AcademicDtos {

    private AcademicDtos() {
    }

    public record GradeRequest(@NotBlank @Size(max = 50) String name, @Size(max = 255) String description) {
    }

    public record GradeResponse(Long id, String name, String description) {

        public static GradeResponse from(Grade grade) {
            return new GradeResponse(grade.getId(), grade.getName(), grade.getDescription());
        }
    }

    public record CreateGroupRequest(@NotNull Long gradeId, @NotBlank @Size(max = 50) String name,
                                     @NotNull @Min(2000) @Max(2200) Integer academicYear) {
    }

    public record UpdateGroupRequest(@NotBlank @Size(max = 50) String name,
                                     @NotNull @Min(2000) @Max(2200) Integer academicYear) {
    }

    public record GroupResponse(Long id, Long gradeId, String gradeName, String name, int academicYear) {

        public static GroupResponse from(GroupView view) {
            return new GroupResponse(view.id(), view.gradeId(), view.gradeName(), view.name(), view.academicYear());
        }
    }

    public record AcademicPeriodRequest(@NotBlank @Size(max = 100) String name, @NotNull LocalDate startDate,
                                        @NotNull LocalDate endDate) {
    }

    public record AcademicPeriodResponse(Long id, String name, LocalDate startDate, LocalDate endDate) {

        public static AcademicPeriodResponse from(AcademicPeriod period) {
            return new AcademicPeriodResponse(period.getId(), period.getName(), period.getStartDate(), period.getEndDate());
        }
    }

    public record TeachingAssignmentRequest(@NotNull Long groupId, @NotNull Long subjectId) {
    }

    public record ActiveRequest(@NotNull Boolean active) {
    }

    public record TeachingAssignmentResponse(Long id, Long groupId, String groupName, String gradeName, int academicYear,
                                             Long subjectId, String subjectName, boolean active) {

        public static TeachingAssignmentResponse from(TeachingAssignmentView v) {
            return new TeachingAssignmentResponse(v.id(), v.groupId(), v.groupName(), v.gradeName(), v.academicYear(),
                    v.subjectId(), v.subjectName(), v.active());
        }
    }

    public record TeachingPeriodRequest(@NotNull Long teachingAssignmentId, @NotNull Long academicPeriodId) {
    }

    public record TeachingPeriodResponse(Long id, Long teachingAssignmentId, Long groupId, String groupName,
                                         String gradeName, int academicYear, Long subjectId, String subjectName,
                                         Long academicPeriodId, String academicPeriodName, LocalDate startDate,
                                         LocalDate endDate, long studentCount) {

        public static TeachingPeriodResponse from(TeachingPeriodView v) {
            return new TeachingPeriodResponse(v.id(), v.teachingAssignmentId(), v.groupId(), v.groupName(),
                    v.gradeName(), v.academicYear(), v.subjectId(), v.subjectName(), v.academicPeriodId(),
                    v.academicPeriodName(), v.startDate(), v.endDate(), v.studentCount());
        }
    }

    public record TeachingPeriodSummaryResponse(Long teachingPeriodId, long studentCount, long activityCount,
                                                long examCount, Grading grading) {

        public record Grading(long expectedGrades, long registeredGrades, int progressPercent) {
        }

        public static TeachingPeriodSummaryResponse from(TeachingPeriodSummary s) {
            return new TeachingPeriodSummaryResponse(s.teachingPeriodId(), s.studentCount(), s.activityCount(),
                    s.examCount(), new Grading(s.expectedGrades(), s.registeredGrades(), s.progressPercent()));
        }
    }

    /** Horas en formato "HH:mm" (se aceptan también segundos, que se descartan). */
    public record ScheduleRequest(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
                                  @NotNull LocalTime endTime, @Size(max = 50) String room) {
    }

    public record ScheduleResponse(Long id, Long teachingPeriodId, DayOfWeek dayOfWeek,
                                   @JsonFormat(pattern = "HH:mm") LocalTime startTime,
                                   @JsonFormat(pattern = "HH:mm") LocalTime endTime, String room) {

        public static ScheduleResponse from(ScheduledClassView v) {
            return new ScheduleResponse(v.scheduleId(), v.teachingPeriodId(), v.dayOfWeek(), v.startTime(),
                    v.endTime(), v.room());
        }
    }

    public record ScheduledClassResponse(Long scheduleId, Long teachingPeriodId, Long subjectId, String subjectName,
                                         Long groupId, String gradeName, String groupName, String academicPeriodName,
                                         @JsonFormat(pattern = "HH:mm") LocalTime startTime,
                                         @JsonFormat(pattern = "HH:mm") LocalTime endTime, String room) {

        public static ScheduledClassResponse from(ScheduledClassView v) {
            return new ScheduledClassResponse(v.scheduleId(), v.teachingPeriodId(), v.subjectId(), v.subjectName(),
                    v.groupId(), v.gradeName(), v.groupName(), v.academicPeriodName(), v.startTime(), v.endTime(),
                    v.room());
        }
    }

    public record TodayScheduleResponse(LocalDate date,
                                        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime serverTime,
                                        String timezone, List<ScheduledClassResponse> classes) {

        public static TodayScheduleResponse from(ScheduleCalendar calendar) {
            ScheduleCalendar.Day day = calendar.days().getFirst();
            return new TodayScheduleResponse(day.date(), calendar.serverTime(), calendar.timezone().getId(),
                    day.classes().stream().map(ScheduledClassResponse::from).toList());
        }
    }

    public record ScheduleDayResponse(LocalDate date, DayOfWeek dayOfWeek, List<ScheduledClassResponse> classes) {
    }

    public record ScheduleRangeResponse(LocalDate from, LocalDate to,
                                        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime serverTime,
                                        String timezone, List<ScheduleDayResponse> days) {

        public static ScheduleRangeResponse from(ScheduleCalendar calendar) {
            List<ScheduleDayResponse> days = calendar.days().stream()
                    .map(d -> new ScheduleDayResponse(d.date(), d.date().getDayOfWeek(),
                            d.classes().stream().map(ScheduledClassResponse::from).toList()))
                    .toList();
            return new ScheduleRangeResponse(days.getFirst().date(), days.getLast().date(), calendar.serverTime(),
                    calendar.timezone().getId(), days);
        }
    }
}
