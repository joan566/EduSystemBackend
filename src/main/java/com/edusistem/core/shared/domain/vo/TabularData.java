package com.edusistem.core.shared.domain.vo;

import java.util.List;

/** Hoja tabular neutral (una hoja): valores String, Number, LocalDate/LocalDateTime o null. */
public record TabularData(String sheetName, List<String> headers, List<List<Object>> rows) {
}
