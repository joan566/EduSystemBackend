package com.edusistem.core.shared.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encabezados de las columnas dinámicas del Excel combinado de un teaching period (hojas Notas/Asistencia de
 * {@code GET /exports/teaching-periods/{id}/full} y {@code POST /imports/teaching-periods/{id}}). El id incrustado
 * en el encabezado (p. ej. "Taller 1 #12 (máx. 5)") es lo que permite reconocer la columna al reimportar, sin
 * depender del orden, del nombre ni del idioma del resto del encabezado (los archivos antiguos usan "(max 5)").
 */
public final class PeriodWorkbookColumns {

    private static final Pattern EMBEDDED_ID = Pattern.compile("#(\\d+)");

    private PeriodWorkbookColumns() {
    }

    public static String activityHeader(String activityName, long activityId, BigDecimal maximumScore) {
        return activityName + " #" + activityId + maximumScoreSuffix(maximumScore);
    }

    /** Encabezado de una actividad en reportes que no se reimportan (sin id). */
    public static String reportActivityHeader(String activityName, BigDecimal maximumScore) {
        return activityName + maximumScoreSuffix(maximumScore);
    }

    public static String sessionHeader(LocalDate sessionDate, long sessionId) {
        return sessionDate + " #" + sessionId;
    }

    /** Id incrustado en un encabezado dinámico (el último "#n", por si el nombre de la actividad también tiene uno). */
    public static Optional<Long> idFromHeader(String header) {
        Matcher matcher = EMBEDDED_ID.matcher(header);
        Long id = null;
        while (matcher.find()) {
            try {
                id = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                id = null;
            }
        }
        return Optional.ofNullable(id);
    }

    private static String maximumScoreSuffix(BigDecimal maximumScore) {
        return " (máx. " + maximumScore.stripTrailingZeros().toPlainString() + ")";
    }
}
