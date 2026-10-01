package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;

/**
 * Aplica en segundo plano el Excel de un tipo de importación. Una fila incorrecta queda en {@link ImportOutcome}; lo
 * que invalida el archivo entero (columnas faltantes, demasiadas filas…) se lanza como excepción de dominio.
 */
public interface ImportProcessor {

    ImportType type();

    ImportOutcome process(ImportBatch batch, byte[] content);
}
