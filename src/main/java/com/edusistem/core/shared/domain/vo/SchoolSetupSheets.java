package com.edusistem.core.shared.domain.vo;

import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Libro "Configuración escolar": lo lee {@code POST /imports/school-setup} y lo generan la plantilla y
 * {@code GET /exports/school-setup}. Aquí están, en un solo lugar, las hojas en orden de procesamiento, sus columnas
 * (claves canónicas en inglés, encabezados en español), cuáles son obligatorias y la ayuda que ve el profesor.
 */
public final class SchoolSetupSheets {

    public static final String ACADEMIC_PERIODS = "AcademicPeriods";
    public static final String ACADEMIC_GRADES = "AcademicGrades";
    public static final String SUBJECTS = "Subjects";
    public static final String GROUPS = "Groups";
    public static final String CLASSES = "Classes";
    public static final String SCHEDULES = "Schedules";
    public static final String STUDENTS = "Students";
    public static final String ACTIVITIES = "Activities";
    public static final String ACTIVITY_GRADES = "ActivityGrades";
    public static final String ATTENDANCE = "Attendance";

    /** Columnas con las que las hojas que dependen de una clase la identifican. */
    public static final List<String> CLASS_KEY_COLUMNS = List.of("grade_name", "group_name", "academic_year",
            "subject_name", "academic_period_name");

    private static final Map<String, String> CLASS_HEADERS = Map.of(
            "grade_name", "Grado",
            "group_name", "Grupo",
            "academic_year", "Año lectivo",
            "subject_name", "Asignatura",
            "academic_period_name", "Periodo",
            "class", "Clase");

    private static final Map<String, Sheet> SHEETS = new LinkedHashMap<>();

    static {
        add(new SheetSpec(ACADEMIC_PERIODS, "Periodos", Map.of(
                        "name", "Nombre",
                        "start_date", "Fecha de inicio",
                        "end_date", "Fecha de fin")),
                List.of("name", "start_date", "end_date"),
                Set.of("name", "start_date", "end_date"),
                Map.of("name", "Nombre del periodo tal como lo usa en las demás hojas. Ej.: 2026-1 o Primer periodo.",
                        "start_date", "Ej.: 2026-01-20 o 20/01/2026.",
                        "end_date", "Ej.: 2026-06-15 o 15/06/2026."));
        add(new SheetSpec(ACADEMIC_GRADES, "Grados", Map.of(
                        "name", "Nombre",
                        "description", "Descripción")),
                List.of("name", "description"),
                Set.of("name"),
                Map.of("name", "Ej.: 10° o Décimo."));
        add(new SheetSpec(SUBJECTS, "Asignaturas", Map.of(
                        "name", "Nombre",
                        "description", "Descripción")),
                List.of("name", "description"),
                Set.of("name"),
                Map.of("name", "Ej.: Matemáticas."));
        add(new SheetSpec(GROUPS, "Grupos", Map.of(
                        "grade_name", "Grado",
                        "name", "Nombre",
                        "academic_year", "Año lectivo")),
                List.of("grade_name", "name", "academic_year"),
                Set.of("grade_name", "name", "academic_year"),
                Map.of("grade_name", "Debe estar en la hoja Grados (o ya existir en la aplicación).",
                        "name", "Ej.: A, B, 01."));
        add(new SheetSpec(CLASSES, "Clases", withClassHeaders(Map.of(
                        "grading_scale", "Escala",
                        "passing_grade", "Nota mínima",
                        "exams_weight", "% Exámenes",
                        "activities_weight", "% Actividades",
                        "attendance_weight", "% Asistencia"))),
                withClassKey("grading_scale", "passing_grade", "exams_weight", "activities_weight", "attendance_weight"),
                Set.copyOf(CLASS_KEY_COLUMNS),
                classNotes(Map.of(
                        "grading_scale", "Opcional. Escala de notas de la clase: 0-5, 0-10 o 0-100 (o el nombre de una "
                                + "escala propia). Si la deja vacía se usa 0-5.",
                        "passing_grade", "Opcional. Nota mínima para aprobar, dentro de la escala. Ej.: 3.",
                        "exams_weight", "Opcional. Porcentaje de la nota que vale el total de exámenes. Ej.: 40. "
                                + "Los tres porcentajes deben sumar 100 para calcular la nota del periodo.",
                        "activities_weight", "Opcional. Porcentaje de la nota que vale el total de actividades. Ej.: 40.",
                        "attendance_weight", "Opcional. Porcentaje de la nota que vale la asistencia. Ej.: 20. "
                                + "Si deja los tres porcentajes vacíos, no se cambia lo que ya tenga configurado.")));
        add(new SheetSpec(SCHEDULES, "Horarios", withClassHeaders(Map.of(
                        "day_of_week", "Día",
                        "start_time", "Hora de inicio",
                        "end_time", "Hora de fin",
                        "room", "Salón"))),
                withClassKey("day_of_week", "start_time", "end_time", "room"),
                union(CLASS_KEY_COLUMNS, "day_of_week", "start_time", "end_time"),
                classNotes(Map.of(
                        "day_of_week", "Lunes, Martes, Miércoles, Jueves, Viernes, Sábado o Domingo.",
                        "start_time", "Ej.: 07:00 o 7:00 a. m.",
                        "end_time", "Ej.: 08:00. Debe ser posterior a la hora de inicio.",
                        "room", "Opcional. Ej.: Salón 201.")));
        add(new SheetSpec(STUDENTS, "Estudiantes", Map.of(
                        "identification_number", "Número de identificación",
                        "first_name", "Nombres",
                        "last_name", "Apellidos",
                        "email", "Correo electrónico",
                        "grade_name", "Grado",
                        "group_name", "Grupo",
                        "academic_year", "Año lectivo")),
                List.of("identification_number", "first_name", "last_name", "email", "grade_name", "group_name",
                        "academic_year"),
                Set.of("identification_number", "first_name", "last_name", "grade_name", "group_name"),
                Map.of("identification_number", "Documento del estudiante. Con él se le reconoce en las demás hojas.",
                        "email", "Opcional.",
                        "academic_year", "Opcional. Si lo deja vacío se usa el año actual."));
        add(new SheetSpec(ACTIVITIES, "Actividades", withClassHeaders(Map.of(
                        "name", "Nombre",
                        "description", "Descripción",
                        "evaluation_date", "Fecha de evaluación",
                        "maximum_score", "Puntaje máximo",
                        "activity_type", "Tipo"))),
                withClassKey("name", "description", "evaluation_date", "maximum_score", "activity_type"),
                union(CLASS_KEY_COLUMNS, "name", "maximum_score"),
                classNotes(Map.of(
                        "evaluation_date", "Opcional. Ej.: 2026-02-10 09:00.",
                        "maximum_score", "Nota máxima que se puede sacar en la actividad. Ej.: 5.",
                        "activity_type", "Opcional. Ej.: Taller, Tarea, Exposición.")));
        add(new SheetSpec(ACTIVITY_GRADES, "Notas de actividades", withClassHeaders(Map.of(
                        "activity_name", "Actividad",
                        "identification_number", "Número de identificación",
                        "grade", "Nota"))),
                withClassKey("activity_name", "identification_number", "grade"),
                union(CLASS_KEY_COLUMNS, "activity_name", "identification_number", "grade"),
                classNotes(Map.of(
                        "activity_name", "Nombre de la actividad, tal como está en la hoja Actividades.",
                        "grade", "Entre 0 y el puntaje máximo de la actividad.")));
        add(new SheetSpec(ATTENDANCE, "Asistencia", withClassHeaders(Map.of(
                        "session_date", "Fecha de sesión",
                        "identification_number", "Número de identificación",
                        "status", "Estado"))),
                withClassKey("session_date", "identification_number", "status"),
                union(CLASS_KEY_COLUMNS, "session_date", "identification_number", "status"),
                classNotes(Map.of("status", "Presente, Ausente o Excusado.")));
    }

    /** Hoja: especificación de nombres, columnas en orden, obligatorias y ayuda por columna. */
    public record Sheet(SheetSpec spec, List<String> columns, Set<String> required, Map<String, String> notes) {
    }

    private SchoolSetupSheets() {
    }

    /** Nombres canónicos de las hojas en orden de procesamiento (cada una depende de las anteriores). */
    public static List<String> sheetNames() {
        return List.copyOf(SHEETS.keySet());
    }

    /**
     * Ordena elementos que corresponden uno a uno con las hojas (p. ej. importadores o exportadores por hoja) según
     * {@link #sheetNames()}. Falla si falta una hoja, sobra o está repetida.
     */
    public static <T> List<T> inSheetOrder(List<T> items, Function<T, String> sheetNameOf) {
        List<String> order = sheetNames();
        List<String> provided = items.stream().map(sheetNameOf).toList();
        if (provided.size() != order.size() || !new HashSet<>(provided).equals(new HashSet<>(order))) {
            throw new IllegalStateException("Expected exactly one element per school setup sheet " + order
                    + " but got " + provided);
        }
        return items.stream().sorted(Comparator.comparingInt(i -> order.indexOf(sheetNameOf.apply(i)))).toList();
    }

    public static SheetSpec spec(String sheetName) {
        return SHEETS.get(sheetName).spec();
    }

    public static List<String> columns(String sheetName) {
        return SHEETS.get(sheetName).columns();
    }

    /**
     * Hoja lista para escribir con encabezados en español, obligatorias resaltadas, ayudas en los encabezados y listas
     * desplegables (Día, Estado y, si se indican, las escalas disponibles).
     */
    public static TabularData table(String sheetName, List<List<Object>> rows, List<String> scaleOptions) {
        Sheet sheet = SHEETS.get(sheetName);
        Set<Integer> required = new HashSet<>();
        Map<Integer, String> notes = new HashMap<>();
        Map<Integer, List<String>> dropdowns = new HashMap<>();
        for (int i = 0; i < sheet.columns().size(); i++) {
            String column = sheet.columns().get(i);
            if (sheet.required().contains(column)) {
                required.add(i);
            }
            String note = sheet.notes().get(column);
            if (note != null) {
                notes.put(i, note);
            }
            switch (column) {
                case "day_of_week" -> dropdowns.put(i, SpreadsheetVocabulary.DAY_LABELS);
                case "status" -> dropdowns.put(i, SpreadsheetVocabulary.ATTENDANCE_STATUS_LABELS);
                case "grading_scale" -> dropdowns.put(i, scaleOptions);
                default -> {
                }
            }
        }
        return sheet.spec().table(sheet.columns(), rows).withHints(new TabularData.Hints(required, notes, dropdowns));
    }

    /** Primera hoja del libro: explica cómo llenarlo. El importador la ignora. */
    public static TabularData instructions() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("Cómo usar este archivo", "Llene las hojas que necesite (todas son opcionales) y súbalo en "
                + "la aplicación. Puede subirlo varias veces: lo que ya existe no se duplica."));
        rows.add(List.of("Columnas amarillas", "Son obligatorias. Pase el mouse sobre el título de una columna para ver "
                + "un ejemplo de qué escribir."));
        rows.add(List.of("Nombres", "Las hojas se relacionan por nombre: el grado, grupo, asignatura y periodo de una "
                + "clase deben escribirse igual que en sus propias hojas (o como ya estén en la aplicación)."));
        rows.add(List.of("Fechas y horas", "Fechas como 2026-03-15 o 15/03/2026. Horas como 07:00 o 7:00 a. m."));
        rows.add(List.of("Periodos", "Periodos académicos (por ejemplo 2026-1) con su fecha de inicio y de fin."));
        rows.add(List.of("Grados", "Grados que dicta (por ejemplo 10°)."));
        rows.add(List.of("Asignaturas", "Materias que dicta."));
        rows.add(List.of("Grupos", "Cursos de cada grado en un año lectivo (por ejemplo 10° A 2026)."));
        rows.add(List.of("Clases", "Qué asignatura dicta a qué grupo en qué periodo. Aquí también se configura la "
                + "nota: escala (0-5 si la deja vacía), nota mínima para aprobar y cuánto vale cada parte (% Exámenes, "
                + "% Actividades, % Asistencia). Los porcentajes deben sumar 100 para que la aplicación calcule la "
                + "nota del periodo."));
        rows.add(List.of("Horarios", "Bloques semanales de cada clase: día, hora de inicio, hora de fin y salón. "
                + "No puede tener dos clases a la misma hora."));
        rows.add(List.of("Estudiantes", "Estudiantes y el grupo en el que están matriculados."));
        rows.add(List.of("Actividades", "Actividades calificables de cada clase (talleres, tareas...)."));
        rows.add(List.of("Notas de actividades", "Nota de cada estudiante en cada actividad."));
        rows.add(List.of("Asistencia", "Asistencia de cada estudiante por fecha: Presente, Ausente o Excusado."));
        rows.add(List.of("Errores", "Si alguna fila tiene un problema, las demás se guardan igual y la aplicación le "
                + "indica la hoja, la fila y qué corregir."));
        return new TabularData("Instrucciones", List.of("Tema", "Detalle"), rows);
    }

    private static void add(SheetSpec spec, List<String> columns, Set<String> required, Map<String, String> notes) {
        SHEETS.put(spec.name(), new Sheet(spec, columns, required, notes));
    }

    private static Map<String, String> withClassHeaders(Map<String, String> headers) {
        Map<String, String> all = new LinkedHashMap<>(CLASS_HEADERS);
        all.putAll(headers);
        return all;
    }

    private static List<String> withClassKey(String... columns) {
        List<String> all = new ArrayList<>(CLASS_KEY_COLUMNS);
        all.addAll(List.of(columns));
        return all;
    }

    private static Set<String> union(List<String> base, String... more) {
        Set<String> all = new HashSet<>(base);
        all.addAll(List.of(more));
        return all;
    }

    private static Map<String, String> classNotes(Map<String, String> notes) {
        Map<String, String> all = new HashMap<>(notes);
        all.put("grade_name", "Grado, grupo, año lectivo, asignatura y periodo identifican la clase (hoja Clases).");
        return all;
    }
}
