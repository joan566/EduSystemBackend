package com.edusistem.core.imports.application.support;

import com.edusistem.core.attendance.domain.enums.AttendanceStatus;
import com.edusistem.core.imports.domain.vo.SpreadsheetRow;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Lee y valida las celdas de una fila. Cada celda inválida agrega un error (en español, referenciando la columna) a
 * {@link RowErrors} y devuelve {@code null}; quien llama compara {@link RowErrors#size()} antes y después para saber
 * si la fila falló.
 */
public final class RowReader {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2200;

    private final SpreadsheetRow row;
    private final SheetSpec spec;
    private final UnaryOperator<String> columnLabel;
    private final RowErrors errors;

    private RowReader(SpreadsheetRow row, SheetSpec spec, UnaryOperator<String> columnLabel, RowErrors errors) {
        this.row = row;
        this.spec = spec;
        this.columnLabel = columnLabel;
        this.errors = errors;
    }

    /** Los errores referencian la columna como "Hoja:Columna" (libros de varias hojas). */
    public static RowReader of(SpreadsheetRow row, SheetSpec spec, RowErrors errors) {
        return new RowReader(row, spec, spec::ref, errors);
    }

    /** Los errores referencian solo el encabezado de la columna (libros de una sola hoja). */
    public static RowReader headerOnly(SpreadsheetRow row, SheetSpec spec, RowErrors errors) {
        return new RowReader(row, spec, spec::header, errors);
    }

    public int rowNumber() {
        return row.rowNumber();
    }

    /** Valor tal cual (recortado; null si la celda está vacía), sin validar. */
    public String raw(String column) {
        return row.get(column);
    }

    public void error(String column, String message) {
        errors.add(row.rowNumber(), columnLabel.apply(column), message);
    }

    /** Error de la fila completa (no de una celda): se referencia solo la hoja. */
    public void sheetError(String message) {
        errors.add(row.rowNumber(), spec.label(), message);
    }

    public String required(String column, int maxLength) {
        String value = row.get(column);
        if (value == null) {
            error(column, ImportMessages.REQUIRED);
            return null;
        }
        if (value.length() > maxLength) {
            error(column, ImportMessages.maxLength(maxLength));
            return null;
        }
        return value;
    }

    public String optional(String column, int maxLength) {
        String value = row.get(column);
        if (value != null && value.length() > maxLength) {
            error(column, ImportMessages.maxLength(maxLength));
            return null;
        }
        return value;
    }

    /** Correo opcional; si es inválido registra el error y lo devuelve igual (la fila ya quedó marcada). */
    public String optionalEmail(String column) {
        String email = row.get(column);
        if (email != null && (email.length() > 255 || !EMAIL.matcher(email).matches())) {
            error(column, ImportMessages.INVALID_EMAIL);
        }
        return email;
    }

    public Integer requiredYear(String column) {
        String raw = row.get(column);
        if (raw == null) {
            error(column, ImportMessages.REQUIRED);
            return null;
        }
        Integer year = parseYear(raw);
        if (year == null) {
            error(column, ImportMessages.INVALID_YEAR);
        }
        return year;
    }

    /** Año opcional: {@code defaultYear} si la celda está vacía o es inválida (en ese caso también registra el error). */
    public int optionalYear(String column, int defaultYear) {
        String raw = row.get(column);
        if (raw == null) {
            return defaultYear;
        }
        Integer year = parseYear(raw);
        if (year == null) {
            error(column, ImportMessages.INVALID_YEAR);
            return defaultYear;
        }
        return year;
    }

    public BigDecimal requiredPositiveDecimal(String column) {
        String raw = row.get(column);
        if (raw == null) {
            error(column, ImportMessages.REQUIRED);
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.signum() <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            error(column, ImportMessages.NOT_POSITIVE);
            return null;
        }
    }

    public LocalDate requiredDate(String column) {
        String raw = row.get(column);
        if (raw == null) {
            error(column, ImportMessages.REQUIRED);
            return null;
        }
        LocalDate date = CellValues.date(raw);
        if (date == null) {
            error(column, ImportMessages.INVALID_DATE);
        }
        return date;
    }

    public LocalDateTime optionalDateTime(String column) {
        String raw = row.get(column);
        if (raw == null) {
            return null;
        }
        LocalDateTime dateTime = CellValues.dateTime(raw);
        if (dateTime == null) {
            error(column, ImportMessages.INVALID_DATE_TIME);
        }
        return dateTime;
    }

    public AttendanceStatus requiredAttendanceStatus(String column) {
        String raw = row.get(column);
        if (raw == null) {
            error(column, ImportMessages.REQUIRED);
            return null;
        }
        Optional<AttendanceStatus> status = attendanceStatus(raw);
        if (status.isEmpty()) {
            error(column, ImportMessages.INVALID_ATTENDANCE);
            return null;
        }
        return status.get();
    }

    /** "Presente"/"Ausente"/"Excusado" (o su nombre en inglés) al estado de asistencia. */
    public static Optional<AttendanceStatus> attendanceStatus(String raw) {
        return SpreadsheetVocabulary.attendanceStatusName(raw).map(AttendanceStatus::valueOf);
    }

    private static Integer parseYear(String raw) {
        try {
            int year = Integer.parseInt(raw);
            return year < MIN_YEAR || year > MAX_YEAR ? null : year;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
