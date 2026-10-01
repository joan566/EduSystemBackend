package com.edusistem.core.imports.application.support;

import com.edusistem.core.imports.domain.vo.ParsedSheet;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import java.util.List;

/** Validación de encabezados: una columna obligatoria que falta aborta la importación completa. */
public final class SheetColumns {

    private SheetColumns() {
    }

    /** Falla con la primera columna de {@code columns} (en ese orden) que no esté en la hoja. */
    public static void require(ParsedSheet sheet, SheetSpec spec, List<String> columns) {
        for (String column : columns) {
            if (!sheet.headers().contains(column)) {
                throw new InvalidRequestException("MISSING_COLUMNS",
                        ImportMessages.missingColumn(spec.label(), spec.header(column)));
            }
        }
    }
}
