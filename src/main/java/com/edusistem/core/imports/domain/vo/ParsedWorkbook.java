package com.edusistem.core.imports.domain.vo;

import java.util.Map;
import java.util.Optional;

/** Libro con varias hojas ya parseadas, indexadas por el nombre exacto de la hoja. */
public record ParsedWorkbook(Map<String, ParsedSheet> sheets) {

    public Optional<ParsedSheet> sheet(String name) {
        return Optional.ofNullable(sheets.get(name));
    }
}
