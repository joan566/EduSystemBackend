package com.edusistem.core.gradebook.domain.vo;

/** Especialización de una evaluación: define cómo se obtiene la nota de cada estudiante. */
public enum EvaluationType {
    /** Examen de selección múltiple, calificado por escaneo. */
    EXAM,
    /** Actividad calificada manualmente (admite rúbrica). */
    ACTIVITY,
    /** Sesión de asistencia (presente = puntaje máximo, ausente = 0, excusa = no cuenta). */
    ATTENDANCE
}
