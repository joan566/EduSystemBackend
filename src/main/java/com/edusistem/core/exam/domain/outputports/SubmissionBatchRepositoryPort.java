package com.edusistem.core.exam.domain.outputports;

import com.edusistem.core.exam.domain.entity.SubmissionBatch;
import com.edusistem.core.exam.domain.entity.SubmissionBatchPage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubmissionBatchRepositoryPort {

    SubmissionBatch save(SubmissionBatch batch);

    SubmissionBatchPage savePage(SubmissionBatchPage page);

    Optional<SubmissionBatch> findById(Long id);

    /** Más recientes primero. */
    List<SubmissionBatch> findByExamId(Long examId);

    /** En orden de página. */
    List<SubmissionBatchPage> findPages(Long batchId);

    /** COMPLETED o FAILED antes de {@code cutoff} que aún conservan su PDF. */
    List<SubmissionBatch> findFinishedWithFileBefore(LocalDateTime cutoff);

    /** QUEUED o PROCESSING: interrumpidos por un reinicio. */
    List<SubmissionBatch> findUnfinished();
}
