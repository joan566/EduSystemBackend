package com.edusistem.core.exam.infrastructure.adapter;

import com.edusistem.core.exam.domain.entity.SubmissionBatch;
import com.edusistem.core.exam.domain.entity.SubmissionBatchPage;
import com.edusistem.core.exam.domain.enums.SubmissionBatchStatus;
import com.edusistem.core.exam.domain.outputports.SubmissionBatchRepositoryPort;
import com.edusistem.core.exam.infrastructure.mapper.SubmissionBatchMapper;
import com.edusistem.core.exam.infrastructure.repository.SpringDataSubmissionBatchPageRepository;
import com.edusistem.core.exam.infrastructure.repository.SpringDataSubmissionBatchRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SubmissionBatchRepositoryAdapter implements SubmissionBatchRepositoryPort {

    private final SpringDataSubmissionBatchRepository batches;
    private final SpringDataSubmissionBatchPageRepository pages;
    private final SubmissionBatchMapper mapper;

    public SubmissionBatchRepositoryAdapter(SpringDataSubmissionBatchRepository batches,
                                            SpringDataSubmissionBatchPageRepository pages,
                                            SubmissionBatchMapper mapper) {
        this.batches = batches;
        this.pages = pages;
        this.mapper = mapper;
    }

    @Override
    public SubmissionBatch save(SubmissionBatch batch) {
        return mapper.toDomain(batches.save(mapper.toEntity(batch)));
    }

    @Override
    public SubmissionBatchPage savePage(SubmissionBatchPage page) {
        return mapper.toDomain(pages.save(mapper.toEntity(page)));
    }

    @Override
    public Optional<SubmissionBatch> findById(Long id) {
        return batches.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SubmissionBatch> findByExamId(Long examId) {
        return batches.findByExamIdOrderByIdDesc(examId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SubmissionBatchPage> findPages(Long batchId) {
        return pages.findByBatchIdOrderByPageNumberAsc(batchId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SubmissionBatch> findFinishedWithFileBefore(LocalDateTime cutoff) {
        return batches.findByStatusInAndFilePathIsNotNullAndCompletedAtBeforeOrderByIdAsc(
                List.of(SubmissionBatchStatus.COMPLETED, SubmissionBatchStatus.FAILED), cutoff)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SubmissionBatch> findUnfinished() {
        return batches.findByStatusInOrderByIdAsc(List.of(SubmissionBatchStatus.QUEUED, SubmissionBatchStatus.PROCESSING))
                .stream().map(mapper::toDomain).toList();
    }
}
