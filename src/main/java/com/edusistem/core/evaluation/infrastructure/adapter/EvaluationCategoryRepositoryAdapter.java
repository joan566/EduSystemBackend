package com.edusistem.core.evaluation.infrastructure.adapter;

import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.domain.outputports.EvaluationCategoryRepositoryPort;
import com.edusistem.core.evaluation.infrastructure.mapper.EvaluationMapper;
import com.edusistem.core.evaluation.infrastructure.repository.SpringDataEvaluationCategoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class EvaluationCategoryRepositoryAdapter implements EvaluationCategoryRepositoryPort {

    private final SpringDataEvaluationCategoryRepository repository;
    private final EvaluationMapper mapper;

    public EvaluationCategoryRepositoryAdapter(SpringDataEvaluationCategoryRepository repository, EvaluationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<EvaluationCategory> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<EvaluationCategory> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<EvaluationCategory> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }
}
