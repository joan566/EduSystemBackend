package com.edusistem.core.shared.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Encabezados de las columnas dinámicas del Excel combinado de un teaching period (hojas Grades/Attendance de
 * {@code GET /exports/teaching-periods/{id}/full} y {@code POST /imports/teaching-periods/{id}}). El id incrustado
 * en el encabezado (p. ej. "Taller 1 #12 (max 5)") es lo que permite reconocer la columna al reimportar, sin
 * depender del orden ni de que el nombre no haya cambiado.
 */
public final class PeriodWorkbookColumns {

    private PeriodWorkbookColumns() {
    }

    public static String activityHeader(String activityName, long activityId, BigDecimal maximumScore) {
        return activityName + " #" + activityId + " (max " + maximumScore.stripTrailingZeros().toPlainString() + ")";
    }

    public static String sessionHeader(LocalDate sessionDate, long sessionId) {
        return sessionDate + " #" + sessionId;
    }
}
