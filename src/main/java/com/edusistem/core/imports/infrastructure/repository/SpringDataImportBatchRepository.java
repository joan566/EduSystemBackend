package com.edusistem.core.imports.infrastructure.repository;

import com.edusistem.core.imports.infrastructure.entity.ImportBatchEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataImportBatchRepository extends JpaRepository<ImportBatchEntity, Long> {

    Page<ImportBatchEntity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @Query("""
            select b from ImportBatchEntity b
            where b.completedAt < :cutoff and (b.filePath is not null or b.errorReportPath is not null)
            order by b.id""")
    List<ImportBatchEntity> findCompletedWithFilesBefore(@Param("cutoff") LocalDateTime cutoff);
}
