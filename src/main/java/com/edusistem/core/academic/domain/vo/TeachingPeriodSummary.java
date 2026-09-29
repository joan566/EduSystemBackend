package com.edusistem.core.academic.domain.vo;

/**
 * Progreso general de una clase. {@code expectedGrades}: estudiantes activos × evaluaciones calificables (actividades
 * y exámenes); {@code registeredGrades}: cuántas de esas notas existen (nota de actividad o entrega con nota final).
 */
public record TeachingPeriodSummary(Long teachingPeriodId, long studentCount, long activityCount, long examCount,
                                    long expectedGrades, long registeredGrades, int progressPercent) {

    public static TeachingPeriodSummary of(Long teachingPeriodId, long studentCount, long activityCount, long examCount,
                                           long registeredGrades) {
        long expected = studentCount * (activityCount + examCount);
        int percent = expected == 0 ? 0 : (int) Math.round(registeredGrades * 100.0 / expected);
        return new TeachingPeriodSummary(teachingPeriodId, studentCount, activityCount, examCount, expected,
                registeredGrades, percent);
    }
}
