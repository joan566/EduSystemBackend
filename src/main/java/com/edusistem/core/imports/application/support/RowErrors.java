package com.edusistem.core.imports.application.support;

import com.edusistem.core.imports.domain.vo.ImportRowError;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Errores acumulados durante una importación y filas que fallaron. Una fila se identifica por "Hoja#fila" para que la
 * misma fila de dos hojas distintas cuente dos veces.
 */
public final class RowErrors {

    private final List<ImportRowError> errors = new ArrayList<>();
    private final Set<String> failedRows = new HashSet<>();

    public void add(int rowNumber, String column, String message) {
        errors.add(new ImportRowError(rowNumber, column, message));
    }

    /** Cantidad de errores hasta ahora; quien valida una fila la compara antes y después para saber si falló. */
    public int size() {
        return errors.size();
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public void markFailed(String sheetName, int rowNumber) {
        failedRows.add(sheetName + "#" + rowNumber);
    }

    public int failedRows() {
        return failedRows.size();
    }

    public List<ImportRowError> list() {
        return errors;
    }
}
