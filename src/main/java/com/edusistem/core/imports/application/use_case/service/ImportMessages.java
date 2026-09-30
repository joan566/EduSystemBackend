package com.edusistem.core.imports.application.use_case.service;

import java.math.BigDecimal;

/** Mensajes visibles para el usuario de las importaciones de Excel (se muestran tal cual en el frontend). */
final class ImportMessages {

    static final String ONLY_XLSX = "Solo se admiten archivos de Excel (.xlsx)";
    static final String REQUIRED = "Obligatorio";
    static final String INVALID_EMAIL = "Correo electrónico inválido";
    static final String INVALID_YEAR = "Debe ser un año entre 2000 y 2200";
    static final String NOT_A_NUMBER = "Debe ser un número";
    static final String NOT_POSITIVE = "Debe ser un número mayor que 0";
    static final String INVALID_DATE = "Debe ser una fecha con formato aaaa-mm-dd";
    static final String INVALID_DATE_TIME = "Debe ser una fecha y hora con formato aaaa-mm-dd hh:mm";
    static final String INVALID_ATTENDANCE = "Debe ser Presente, Ausente o Excusado";
    static final String DUPLICATED_IN_SHEET = "Duplicado en la hoja";

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

    static String couldNotCreate(String cause) {
        return "No se pudo crear: " + cause;
    }

    static String couldNotSave(String cause) {
        return "No se pudo guardar: " + cause;
    }
}
