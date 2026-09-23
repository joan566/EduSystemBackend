package com.edusistem.core.academic.application.use_case.dtos;

import java.time.LocalDate;

public final class AcademicCommands {

    private AcademicCommands() {
    }

    public record CreateGrade(Long actorId, String name, String description) {
    }

    public record UpdateGrade(Long actorId, Long gradeId, String name, String description) {
    }

    public record CreateGroup(Long actorId, Long gradeId, String name, int academicYear) {
    }

    public record UpdateGroup(Long actorId, Long groupId, String name, int academicYear) {
    }

    public record SavePeriod(Long actorId, Long periodId, String name, LocalDate startDate, LocalDate endDate) {
    }

    public record CreateTeachingAssignment(Long teacherId, Long groupId, Long subjectId) {
    }

    public record CreateTeachingPeriod(Long teacherId, Long teachingAssignmentId, Long academicPeriodId) {
    }
}
