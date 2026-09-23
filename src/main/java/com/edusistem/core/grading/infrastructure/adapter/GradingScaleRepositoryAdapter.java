package com.edusistem.core.grading.infrastructure.adapter;

import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.outputports.GradingScaleRepositoryPort;
import com.edusistem.core.grading.infrastructure.mapper.GradingMapper;
import com.edusistem.core.grading.infrastructure.repository.SpringDataGradingScaleRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class GradingScaleRepositoryAdapter implements GradingScaleRepositoryPort {

    private final SpringDataGradingScaleRepository repository;
    private final GradingMapper mapper;

    public GradingScaleRepositoryAdapter(SpringDataGradingScaleRepository repository, GradingMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public GradingScale save(GradingScale scale) {
        return mapper.toDomain(repository.save(mapper.toEntity(scale)));
    }

    @Override
    public Optional<GradingScale> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<GradingScale> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(mapper::toDomain).toList();
    }
}
