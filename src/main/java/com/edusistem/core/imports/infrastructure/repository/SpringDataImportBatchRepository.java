package com.edusistem.core.imports.infrastructure.repository;

import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.infrastructure.entity.ImportBatchEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataImportBatchRepository extends JpaRepository<ImportBatchEntity, Long> {

    Page<ImportBatchEntity> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @Query("""
            select b from ImportBatchEntity b
            where b.completedAt < :cutoff and (b.filePath is not null or b.errorReportPath is not null)
            order by b.id""")
    List<ImportBatchEntity> findCompletedWithFilesBefore(@Param("cutoff") LocalDateTime cutoff);

    /** Solo las encoladas: las síncronas antiguas (sin tipo) no se pueden reanudar. */
    List<ImportBatchEntity> findByStatusInAndImportTypeIsNotNullOrderByIdAsc(Collection<ImportStatus> statuses);

    boolean existsByUserIdAndStatusInAndImportTypeIsNotNull(Long userId, Collection<ImportStatus> statuses);

    @Modifying
    @Query("""
            update ImportBatchEntity b
            set b.processedRows = :processed, b.totalRows = :total, b.currentStep = :step
            where b.id = :id""")
    void updateProgress(@Param("id") Long id, @Param("processed") int processed, @Param("total") int total,
                        @Param("step") String step);
}
