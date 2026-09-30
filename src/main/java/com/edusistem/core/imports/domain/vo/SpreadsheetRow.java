package com.edusistem.core.imports.domain.vo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Fila de datos de la hoja (número de fila Excel 1-based; claves = encabezados normalizados). */
public record SpreadsheetRow(int rowNumber, Map<String, String> values) {

    public String get(String column) {
        String value = values.get(column);
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Renombra las claves; si dos columnas terminan con la misma clave, gana la primera que tenga valor. */
    public SpreadsheetRow renameColumns(UnaryOperator<String> rename) {
        Map<String, String> renamed = new LinkedHashMap<>();
        values.forEach((column, value) -> renamed.merge(rename.apply(column), value,
                (current, other) -> current == null || current.isBlank() ? other : current));
        return new SpreadsheetRow(rowNumber, renamed);
    }
}
