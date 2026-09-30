package com.edusistem.core.imports.domain.vo;

import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import java.util.Map;
import java.util.Optional;

/** Libro con varias hojas ya parseadas, indexadas por el nombre de la hoja tal como viene en el archivo. */
public record ParsedWorkbook(Map<String, ParsedSheet> sheets) {

    /** Hoja con nombre en español o en inglés, con sus encabezados ya traducidos a las claves canónicas. */
    public Optional<ParsedSheet> sheet(SheetSpec spec) {
        return sheets.entrySet().stream()
                .filter(e -> spec.isNamed(e.getKey()))
                .map(e -> e.getValue().canonicalize(spec))
                .findFirst();
    }
}
