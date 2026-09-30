package com.edusistem.core.shared.domain.vo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hoja tabular neutral (una hoja): valores String, Number, LocalDate/LocalDateTime/LocalTime o null. {@code hints}
 * agrega ayudas opcionales para quien llena el Excel a mano (columnas obligatorias resaltadas, comentarios en los
 * encabezados y listas desplegables).
 */
public record TabularData(String sheetName, List<String> headers, List<List<Object>> rows, Hints hints) {

    public TabularData {
        hints = hints == null ? Hints.NONE : hints;
    }

    public TabularData(String sheetName, List<String> headers, List<List<Object>> rows) {
        this(sheetName, headers, rows, Hints.NONE);
    }

    public TabularData withHints(Hints hints) {
        return new TabularData(sheetName, headers, rows, hints);
    }

    /**
     * Ayudas por índice de columna (0-based). En {@code dropdowns} los valores se sugieren pero no se imponen: el
     * lector acepta también otras formas (sin tildes, en inglés...).
     */
    public record Hints(Set<Integer> requiredColumns, Map<Integer, String> notes, Map<Integer, List<String>> dropdowns) {

        public static final Hints NONE = new Hints(Set.of(), Map.of(), Map.of());
    }
}
