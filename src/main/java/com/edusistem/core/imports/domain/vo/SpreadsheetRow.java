package com.edusistem.core.imports.domain.vo;

import java.util.Map;

/** Fila de datos de la hoja (número de fila Excel 1-based; claves = encabezados normalizados). */
public record SpreadsheetRow(int rowNumber, Map<String, String> values) {

    public String get(String column) {
        String value = values.get(column);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
