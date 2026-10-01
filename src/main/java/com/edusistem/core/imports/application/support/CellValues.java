package com.edusistem.core.imports.application.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpretación tolerante de lo que un profesor escribe a mano en una celda. Cada método devuelve {@code null} si el
 * texto no se puede interpretar; quien llama decide el mensaje de error.
 */
public final class CellValues {

    private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d/M/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    /** "7:00", "07:00", "7:00:00", "7:00 am", "7:00 p. m.", "7 pm". */
    private static final Pattern TIME = Pattern.compile(
            "^(\\d{1,2})(?:[:.](\\d{2}))?(?::\\d{2})?\\s*(?:([ap])\\.?\\s*m\\.?)?$", Pattern.CASE_INSENSITIVE);

    private CellValues() {
    }

    /** "2026-03-15", "15/03/2026" o "15-03-2026" (día primero, como se escribe en español). */
    public static LocalDate date(String raw) {
        String text = raw.trim();
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(text.replace('-', '/').replace('.', '/'), DAY_MONTH_YEAR);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    /** Fecha con hora opcional ("2026-02-10 09:00", "10/02/2026 9:00 am" o sólo "2026-02-10"). */
    public static LocalDateTime dateTime(String raw) {
        String text = raw.trim().replace('T', ' ');
        int space = text.indexOf(' ');
        LocalDate date = date(space < 0 ? text : text.substring(0, space));
        if (date == null) {
            return null;
        }
        if (space < 0) {
            return date.atStartOfDay();
        }
        LocalTime time = time(text.substring(space + 1));
        return time == null ? null : date.atTime(time);
    }

    public static LocalTime time(String raw) {
        Matcher m = TIME.matcher(raw.trim().replace(' ', ' '));
        if (!m.matches()) {
            return null;
        }
        int hour = Integer.parseInt(m.group(1));
        int minute = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        String half = m.group(3);
        if (half != null) {
            if (hour < 1 || hour > 12) {
                return null;
            }
            hour = hour % 12 + (half.toLowerCase(Locale.ROOT).equals("p") ? 12 : 0);
        }
        if (hour > 23 || minute > 59) {
            return null;
        }
        return LocalTime.of(hour, minute);
    }

    /** Número con punto o coma decimal ("3.5" o "3,5"). */
    public static BigDecimal decimal(String raw) {
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Porcentaje: "30", "30%", "30 %" o "30,5" → 30 / 30.5. */
    public static BigDecimal percent(String raw) {
        String text = raw.trim();
        if (text.endsWith("%")) {
            text = text.substring(0, text.length() - 1);
        }
        return decimal(text);
    }
}
