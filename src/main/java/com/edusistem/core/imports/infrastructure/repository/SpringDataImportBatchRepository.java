package com.edusistem.core.imports.infrastructure.repository;

import com.edusistem.core.imports.infrastructure.entity.ImportBatchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataImportBatchRepository extends JpaRepository<ImportBatchEntity, Long> {

    Page<ImportBatchEntity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
