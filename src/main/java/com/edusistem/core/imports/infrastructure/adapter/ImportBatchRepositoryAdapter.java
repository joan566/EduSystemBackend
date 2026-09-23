package com.edusistem.core.imports.infrastructure.adapter;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.outputports.ImportBatchRepositoryPort;
import com.edusistem.core.imports.infrastructure.mapper.ImportBatchMapper;
import com.edusistem.core.imports.infrastructure.repository.SpringDataImportBatchRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ImportBatchRepositoryAdapter implements ImportBatchRepositoryPort {

    private final SpringDataImportBatchRepository repository;
    private final ImportBatchMapper mapper;

    public ImportBatchRepositoryAdapter(SpringDataImportBatchRepository repository, ImportBatchMapper mapper) {
        this.repository = repository;
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
}
