package com.edusistem.core.imports.domain.vo;

import com.edusistem.core.shared.domain.vo.SpreadsheetVocabulary.SheetSpec;
import java.util.List;

public record ParsedSheet(List<String> headers, List<SpreadsheetRow> rows) {

    /** Misma hoja con los encabezados conocidos (en español o en inglés) traducidos a su clave canónica. */
    public ParsedSheet canonicalize(SheetSpec spec) {
        return new ParsedSheet(headers.stream().map(spec::canonicalColumn).toList(),
                rows.stream().map(r -> r.renameColumns(spec::canonicalColumn)).toList());
    }
}
