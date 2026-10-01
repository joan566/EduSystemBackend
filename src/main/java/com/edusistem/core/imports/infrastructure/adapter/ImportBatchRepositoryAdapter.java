package com.edusistem.core.imports.infrastructure.adapter;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportStatus;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.domain.vo.ImportRowError;
import com.edusistem.core.imports.infrastructure.entity.ImportBatchErrorEntity;
import com.edusistem.core.imports.infrastructure.mapper.ImportBatchMapper;
import com.edusistem.core.imports.infrastructure.repository.SpringDataImportBatchErrorRepository;
import com.edusistem.core.imports.infrastructure.repository.SpringDataImportBatchRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ImportBatchRepositoryAdapter implements ImportBatchRepositoryPort {

    private static final Set<ImportStatus> UNFINISHED = Set.of(ImportStatus.QUEUED, ImportStatus.PROCESSING);

    private final SpringDataImportBatchRepository repository;
    private final SpringDataImportBatchErrorRepository errors;
    private final ImportBatchMapper mapper;

    public ImportBatchRepositoryAdapter(SpringDataImportBatchRepository repository,
                                        SpringDataImportBatchErrorRepository errors, ImportBatchMapper mapper) {
        this.repository = repository;
        this.errors = errors;
        this.mapper = mapper;
    }

    @Override
    public ImportBatch save(ImportBatch batch) {
        return mapper.toDomain(repository.save(mapper.toEntity(batch)));
    }

    @Override
    public Optional<ImportBatch> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<ImportBatch> findByUserId(Long userId, PageQuery page) {
        return PageMapper.toResult(repository.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageMapper.pageable(page)),
                mapper::toDomain);
    }

    @Override
    public List<ImportBatch> findCompletedWithFilesBefore(LocalDateTime cutoff) {
        return repository.findCompletedWithFilesBefore(cutoff).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ImportBatch> findUnfinished() {
        return repository.findByStatusInAndImportTypeIsNotNullOrderByIdAsc(UNFINISHED).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsUnfinishedByUserId(Long userId) {
        return repository.existsByUserIdAndStatusInAndImportTypeIsNotNull(userId, UNFINISHED);
    }

    @Override
    public void saveErrors(Long batchId, List<ImportRowError> rowErrors) {
        errors.saveAll(rowErrors.stream().map(e -> {
            ImportBatchErrorEntity entity = new ImportBatchErrorEntity();
            entity.setBatchId(batchId);
            entity.setRowNumber(e.rowNumber());
            entity.setColumnName(e.column());
            entity.setMessage(e.message());
            return entity;
        }).toList());
    }

    @Override
    public List<ImportRowError> findErrors(Long batchId) {
        return errors.findByBatchIdOrderByIdAsc(batchId).stream()
                .map(e -> new ImportRowError(e.getRowNumber(), e.getColumnName(), e.getMessage())).toList();
    }
}
