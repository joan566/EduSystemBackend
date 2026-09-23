package com.edusistem.core.imports.application.use_case.dtos;

public final class ImportCommands {

    private ImportCommands() {
    }

    public record ImportStudents(Long teacherId, String fileName, byte[] content) {
    }

    /** Excel combinado (hojas Students/Grades/Attendance, todas opcionales) para un teaching period. */
    public record ImportTeachingPeriodData(Long teacherId, Long teachingPeriodId, String fileName, byte[] content) {
    }

    /**
     * Excel combinado de configuración completa (9 hojas, todas opcionales): AcademicPeriods, AcademicGrades,
     * Subjects, Groups, Classes, Students, Activities, ActivityGrades, Attendance.
     */
    public record ImportSchoolSetup(Long teacherId, String fileName, byte[] content) {
    }
}
