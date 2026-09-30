package com.edusistem.core.academic.application.use_case.dtos;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public final class AcademicCommands {

    private AcademicCommands() {
    }

    public record CreateGrade(Long teacherId, String name, String description) {
    }

    public record UpdateGrade(Long teacherId, Long gradeId, String name, String description) {
    }

    public record CreateGroup(Long teacherId, Long gradeId, String name, int academicYear) {
    }

    public record UpdateGroup(Long teacherId, Long groupId, String name, int academicYear) {
    }

    public record SavePeriod(Long teacherId, Long periodId, String name, LocalDate startDate, LocalDate endDate) {
    }

    public record CreateTeachingAssignment(Long teacherId, Long groupId, Long subjectId) {
    }

    public record CreateTeachingPeriod(Long teacherId, Long teachingAssignmentId, Long academicPeriodId) {
    }

    /** {@code scheduleId} es null al crear. */
    public record SaveSchedule(Long teacherId, Long teachingPeriodId, Long scheduleId, DayOfWeek dayOfWeek,
                               LocalTime startTime, LocalTime endTime, String room) {
    }
}
