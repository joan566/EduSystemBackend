package com.edusistem.core.exam.infrastructure.repository;

import com.edusistem.core.exam.domain.enums.SubmissionBatchStatus;
import com.edusistem.core.exam.infrastructure.entity.SubmissionBatchEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSubmissionBatchRepository extends JpaRepository<SubmissionBatchEntity, Long> {

    List<SubmissionBatchEntity> findByExamIdOrderByIdDesc(Long examId);

    List<SubmissionBatchEntity> findByStatusInAndFilePathIsNotNullAndCompletedAtBeforeOrderByIdAsc(
            Collection<SubmissionBatchStatus> statuses, LocalDateTime cutoff);

    List<SubmissionBatchEntity> findByStatusInOrderByIdAsc(Collection<SubmissionBatchStatus> statuses);
}
