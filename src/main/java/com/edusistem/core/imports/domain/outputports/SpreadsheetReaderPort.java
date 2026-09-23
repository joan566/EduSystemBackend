package com.edusistem.core.imports.domain.outputports;

import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.imports.domain.vo.ParsedWorkbook;

public interface SpreadsheetReaderPort {

    /**
     * Lee la primera hoja de un .xlsx: la primera fila son encabezados (normalizados a minúsculas con guion bajo) y las
     * filas totalmente vacías se omiten.
     * @throws com.edusistem.core.shared.domain.exceptions.InvalidRequestException si el archivo no es un Excel legible
     */
    ParsedSheet read(byte[] content);

    /** Igual que {@link #read}, pero para todas las hojas del libro, indexadas por su nombre exacto. */
    ParsedWorkbook readAll(byte[] content);
}
