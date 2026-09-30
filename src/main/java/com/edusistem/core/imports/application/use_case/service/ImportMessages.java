package com.edusistem.core.imports.application.use_case.service;

import com.edusistem.core.grading.domain.entity.GradingScale;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Mensajes visibles para el usuario de las importaciones de Excel (se muestran tal cual en el frontend). */
final class ImportMessages {

    static final String ONLY_XLSX = "Solo se admiten archivos de Excel (.xlsx)";
    static final String REQUIRED = "Obligatorio";
    static final String INVALID_EMAIL = "Correo electrónico inválido";
    static final String INVALID_YEAR = "Debe ser un año entre 2000 y 2200";
    static final String NOT_A_NUMBER = "Debe ser un número";
    static final String NOT_POSITIVE = "Debe ser un número mayor que 0";
    static final String INVALID_DATE = "Debe ser una fecha, por ejemplo 2026-03-15 o 15/03/2026";
    static final String INVALID_DATE_TIME = "Debe ser una fecha y hora, por ejemplo 2026-03-15 09:00";
    static final String INVALID_ATTENDANCE = "Debe ser Presente, Ausente o Excusado";
    static final String DUPLICATED_IN_SHEET = "Duplicado en la hoja";
    static final String INVALID_DAY = "Debe ser un día de la semana: Lunes, Martes, Miércoles, Jueves, Viernes, Sábado o Domingo";
    static final String INVALID_TIME = "Debe ser una hora, por ejemplo 07:00 o 2:30 p. m.";
    static final String END_BEFORE_START = "La hora de fin debe ser posterior a la hora de inicio";
    static final String INVALID_WEIGHT = "Debe ser un porcentaje entre 0 y 100, por ejemplo 40";
    static final String SCALE_LOCKED = "No se puede cambiar la escala: esta clase ya tiene exámenes calificados";

    private ImportMessages() {
    }

    static String maxLength(int maxLength) {
        return "Debe tener como máximo " + maxLength + " caracteres";
    }

    static String between(BigDecimal maximumScore) {
        return "Debe estar entre 0 y " + maximumScore.stripTrailingZeros().toPlainString();
    }

    static String duplicatedInFile(int firstRow) {
        return "Duplicado en el archivo (aparece primero en la fila " + firstRow + ")";
    }

    static String duplicatedInSheet(int firstRow) {
        return "Duplicado en la hoja (aparece primero en la fila " + firstRow + ")";
    }

    static String tooManyRows(int maxRows) {
        return "El archivo tiene más de " + maxRows + " filas";
    }

    static String missingColumn(String sheetLabel, String header) {
        return "A la hoja '" + sheetLabel + "' le falta la columna obligatoria: " + header;
    }

    static String unknownScale(List<String> options) {
        return "Escala desconocida. Use una de estas: " + String.join(", ", options);
    }

    static String weightsExceed(BigDecimal total) {
        return "Los porcentajes suman " + total.stripTrailingZeros().toPlainString() + " %; el máximo es 100 %";
    }

    static String outsideScale(GradingScale scale) {
        return "Debe estar entre " + scale.getMinimumValue().stripTrailingZeros().toPlainString() + " y "
                + scale.getMaximumValue().stripTrailingZeros().toPlainString() + " (la escala de la clase)";
    }

    static String scheduleConflict(String otherClass, String day, String start, String end) {
        return "Se cruza con la clase " + otherClass + " del " + day.toLowerCase(Locale.ROOT) + " de " + start + " a "
                + end;
    }

    static String couldNotCreate(String cause) {
        return "No se pudo crear: " + cause;
    }

    static String couldNotSave(String cause) {
        return "No se pudo guardar: " + cause;
    }
}
