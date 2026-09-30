package com.edusistem.core.shared.domain.vo;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Textos visibles de los Excel (hojas, encabezados y valores) en español. El código trabaja siempre con claves
 * canónicas en inglés (p. ej. {@code identification_number}); al escribir se usa la etiqueta en español y al leer se
 * aceptan ambas, de modo que los archivos descargados antes del cambio (en inglés) se siguen pudiendo importar.
 */
public final class SpreadsheetVocabulary {

    /** Libro combinado de un teaching period ({@code GET /exports/teaching-periods/{id}/full}) y exports sueltos. */
    public static final SheetSpec STUDENTS = new SheetSpec("Students", "Estudiantes", Map.of(
            "student_code", "Código",
            "identification_number", "Número de identificación",
            "first_name", "Nombres",
            "last_name", "Apellidos",
            "email", "Correo electrónico"));

    public static final SheetSpec GRADES = new SheetSpec("Grades", "Notas", Map.of(
            "student_code", "Código",
            "identification_number", "Número de identificación",
            "first_name", "Nombres",
            "last_name", "Apellidos",
            "period_grade", "Nota del periodo"));

    public static final SheetSpec ATTENDANCE = new SheetSpec("Attendance", "Asistencia", Map.of(
            "student_code", "Código",
            "identification_number", "Número de identificación",
            "first_name", "Nombres",
            "last_name", "Apellidos",
            "present", "Presentes",
            "absent", "Ausencias",
            "excused", "Excusas",
            "attendance_percent", "% Asistencia"));

    /** Reporte de errores de una importación. */
    public static final SheetSpec ERRORS = new SheetSpec("Errors", "Errores", Map.of(
            "row", "Fila",
            "column", "Columna",
            "error", "Error"));

    private static final Map<String, String> ATTENDANCE_LABELS = Map.of(
            "PRESENT", "Presente",
            "ABSENT", "Ausente",
            "EXCUSED", "Excusado");

    private static final Map<String, String> ATTENDANCE_BY_NORMALIZED = new HashMap<>();

    static {
        ATTENDANCE_LABELS.forEach((status, label) -> {
            ATTENDANCE_BY_NORMALIZED.put(normalize(status), status);
            ATTENDANCE_BY_NORMALIZED.put(normalize(label), status);
        });
    }

    private SpreadsheetVocabulary() {
    }

    /** Forma comparable de un encabezado o nombre de hoja: sin tildes, en minúsculas y con espacios/guiones como "_". */
    public static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    /** Etiqueta en español de un estado de asistencia (nombre del enum, p. ej. "PRESENT" → "Presente"). */
    public static String attendanceLabel(String statusName) {
        return ATTENDANCE_LABELS.getOrDefault(statusName, statusName);
    }

    /** Nombre del enum de asistencia a partir del texto de una celda; acepta español o inglés, con o sin tildes. */
    public static Optional<String> attendanceStatusName(String raw) {
        return Optional.ofNullable(ATTENDANCE_BY_NORMALIZED.get(normalize(raw)));
    }

    /** Hoja de un Excel: nombre canónico, etiqueta en español y etiquetas en español de sus columnas conocidas. */
    public static final class SheetSpec {

        private final String name;
        private final String label;
        private final Map<String, String> headerByColumn;
        private final Map<String, String> columnByNormalized = new HashMap<>();

        public SheetSpec(String name, String label, Map<String, String> headerByColumn) {
            this.name = name;
            this.label = label;
            this.headerByColumn = new LinkedHashMap<>(headerByColumn);
            headerByColumn.forEach((column, header) -> {
                columnByNormalized.put(normalize(column), column);
                columnByNormalized.put(normalize(header), column);
            });
        }

        public String name() {
            return name;
        }

        public String label() {
            return label;
        }

        /** Encabezado en español de una columna canónica (o la columna tal cual si no es conocida, p. ej. dinámica). */
        public String header(String column) {
            return headerByColumn.getOrDefault(column, column);
        }

        public List<String> headers(List<String> columns) {
            return columns.stream().map(this::header).toList();
        }

        /** Hoja lista para escribir, con la etiqueta y los encabezados en español. */
        public TabularData table(List<String> columns, List<List<Object>> rows) {
            return new TabularData(label, headers(columns), rows);
        }

        /** Referencia "Hoja:Columna" en español para mensajes de error. */
        public String ref(String column) {
            return label + ":" + header(column);
        }

        /** Si un nombre de hoja corresponde a esta hoja, en español o en inglés (sin distinguir mayúsculas ni tildes). */
        public boolean isNamed(String sheetName) {
            String normalized = normalize(sheetName);
            return normalized.equals(normalize(name)) || normalized.equals(normalize(label));
        }

        /** Clave canónica de un encabezado ya normalizado; si no es una columna conocida se devuelve igual. */
        public String canonicalColumn(String normalizedHeader) {
            return columnByNormalized.getOrDefault(normalizedHeader, normalizedHeader);
        }
    }
}
