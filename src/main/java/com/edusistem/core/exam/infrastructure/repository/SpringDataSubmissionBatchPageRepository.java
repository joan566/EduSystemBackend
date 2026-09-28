package com.edusistem.core.exam.infrastructure.repository;

import com.edusistem.core.exam.infrastructure.entity.SubmissionBatchPageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSubmissionBatchPageRepository extends JpaRepository<SubmissionBatchPageEntity, Long> {

    List<SubmissionBatchPageEntity> findByBatchIdOrderByPageNumberAsc(Long batchId);
}
