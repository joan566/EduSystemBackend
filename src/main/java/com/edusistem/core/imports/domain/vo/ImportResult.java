package com.edusistem.core.imports.domain.vo;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import java.util.List;

/** {@code errors} está acotada a las primeras filas con error; el reporte completo se descarga en Excel. */
public record ImportResult(ImportBatch batch, List<ImportRowError> errors, boolean errorsTruncated) {
}
