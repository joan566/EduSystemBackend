package com.edusistem.core.imports.infrastructure.repository;

import com.edusistem.core.imports.infrastructure.entity.ImportBatchErrorEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataImportBatchErrorRepository extends JpaRepository<ImportBatchErrorEntity, Long> {

    List<ImportBatchErrorEntity> findByBatchIdOrderByIdAsc(Long batchId);
}
