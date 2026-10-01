package com.edusistem.core.imports.application.support;

import com.edusistem.core.grading.domain.entity.GradingScale;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Mensajes visibles para el usuario de las importaciones de Excel (se muestran tal cual en el frontend). */
public final class ImportMessages {

    public static final String ONLY_XLSX = "Solo se admiten archivos de Excel (.xlsx)";
    public static final String REQUIRED = "Obligatorio";
    public static final String INVALID_EMAIL = "Correo electrónico inválido";
    public static final String INVALID_YEAR = "Debe ser un año entre 2000 y 2200";
    public static final String NOT_A_NUMBER = "Debe ser un número";
    public static final String NOT_POSITIVE = "Debe ser un número mayor que 0";
    public static final String INVALID_DATE = "Debe ser una fecha, por ejemplo 2026-03-15 o 15/03/2026";
    public static final String INVALID_DATE_TIME = "Debe ser una fecha y hora, por ejemplo 2026-03-15 09:00";
    public static final String INVALID_ATTENDANCE = "Debe ser Presente, Ausente o Excusado";
    public static final String DUPLICATED_IN_SHEET = "Duplicado en la hoja";
    public static final String INVALID_DAY = "Debe ser un día de la semana: Lunes, Martes, Miércoles, Jueves, Viernes, Sábado o Domingo";
    public static final String INVALID_TIME = "Debe ser una hora, por ejemplo 07:00 o 2:30 p. m.";
    public static final String END_BEFORE_START = "La hora de fin debe ser posterior a la hora de inicio";
    public static final String INVALID_WEIGHT = "Debe ser un porcentaje entre 0 y 100, por ejemplo 40";
    public static final String SCALE_LOCKED = "No se puede cambiar la escala: esta clase ya tiene exámenes calificados";
    public static final String STUDENT_NOT_IN_GROUP = "No hay un estudiante activo con este número de identificación en el grupo";
    public static final String STUDENT_NOT_IN_CLASS_GROUP =
            "No hay un estudiante activo con este número de identificación en el grupo de esa clase";
    public static final String SESSION_NOT_CREATED = "No se pudo crear la sesión de asistencia";

    private ImportMessages() {
    }

    public static String maxLength(int maxLength) {
        return "Debe tener como máximo " + maxLength + " caracteres";
    }

    public static String between(BigDecimal maximumScore) {
        return "Debe estar entre 0 y " + maximumScore.stripTrailingZeros().toPlainString();
    }

    public static String duplicatedInFile(int firstRow) {
        return "Duplicado en el archivo (aparece primero en la fila " + firstRow + ")";
    }

    public static String duplicatedInSheet(int firstRow) {
        return "Duplicado en la hoja (aparece primero en la fila " + firstRow + ")";
    }

    public static String tooManyRows(int maxRows) {
        return "El archivo tiene más de " + maxRows + " filas";
    }

    public static String missingColumn(String sheetLabel, String header) {
        return "A la hoja '" + sheetLabel + "' le falta la columna obligatoria: " + header;
    }

    public static String unknownScale(List<String> options) {
        return "Escala desconocida. Use una de estas: " + String.join(", ", options);
    }

    public static String weightsExceed(BigDecimal total) {
        return "Los porcentajes suman " + total.stripTrailingZeros().toPlainString() + " %; el máximo es 100 %";
    }

    public static String outsideScale(GradingScale scale) {
        return "Debe estar entre " + scale.getMinimumValue().stripTrailingZeros().toPlainString() + " y "
                + scale.getMaximumValue().stripTrailingZeros().toPlainString() + " (la escala de la clase)";
    }

    public static String scheduleConflict(String otherClass, String day, String start, String end) {
        return "Se cruza con la clase " + otherClass + " del " + day.toLowerCase(Locale.ROOT) + " de " + start + " a "
                + end;
    }

    public static String couldNotCreate(String cause) {
        return "No se pudo crear: " + cause;
    }

    public static String couldNotSave(String cause) {
        return "No se pudo guardar: " + cause;
    }

    public static String gradeNotFound(String gradeName, String sheetLabel) {
        return "El grado '" + gradeName + "' no existe (agréguelo en la hoja " + sheetLabel + ")";
    }

    public static String groupNotFound(String groupName, String gradeName, int academicYear, String sheetLabel) {
        return "El grupo '" + groupName + "' no existe en el grado '" + gradeName + "' para " + academicYear
                + " (agréguelo en la hoja " + sheetLabel + ")";
    }

    public static String subjectNotFound(String subjectName, String sheetLabel) {
        return "La asignatura '" + subjectName + "' no existe (agréguela en la hoja " + sheetLabel + ")";
    }

    public static String periodNotFound(String periodName, String sheetLabel) {
        return "El periodo '" + periodName + "' no existe (agréguelo en la hoja " + sheetLabel + ")";
    }

    public static String activityNotFound(String activityName, String sheetLabel) {
        return "No se encontró la actividad '" + activityName + "' en esa clase (agréguela en la hoja " + sheetLabel + ")";
    }

    public static String classNotFound(String gradeName, String groupName, int academicYear, String subjectName,
                                       String periodName, String sheetLabel) {
        return "No se encontró la clase del grado '" + gradeName + "', grupo '" + groupName + "' (" + academicYear
                + "), asignatura '" + subjectName + "', periodo '" + periodName + "' (agréguela en la hoja " + sheetLabel
                + ")";
    }
}
