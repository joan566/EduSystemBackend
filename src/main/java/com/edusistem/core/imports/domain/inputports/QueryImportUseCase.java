package com.edusistem.core.imports.domain.inputports;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface QueryImportUseCase {

    PageResult<ImportBatch> list(Long teacherId, PageQuery page);

    ImportBatch get(Long teacherId, Long batchId);

    /** Reporte de errores (.xlsx) de una importación. */
    byte[] errorReport(Long teacherId, Long batchId);
}
