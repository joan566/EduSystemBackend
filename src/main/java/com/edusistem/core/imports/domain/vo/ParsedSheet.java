package com.edusistem.core.imports.domain.vo;

import java.util.List;

public record ParsedSheet(List<String> headers, List<SpreadsheetRow> rows) {
}
