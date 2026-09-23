package com.edusistem.core.imports.domain.outputports;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface ImportBatchRepositoryPort {

    ImportBatch save(ImportBatch batch);

    Optional<ImportBatch> findById(Long id);

    PageResult<ImportBatch> findByUserId(Long userId, PageQuery page);
}
