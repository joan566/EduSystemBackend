package com.edusistem.core.evaluation.infrastructure.adapter;

import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.outputports.EvaluationRepositoryPort;
import com.edusistem.core.evaluation.domain.vo.EvaluationView;
import com.edusistem.core.evaluation.infrastructure.mapper.EvaluationMapper;
import com.edusistem.core.evaluation.infrastructure.repository.SpringDataEvaluationRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EvaluationRepositoryAdapter implements EvaluationRepositoryPort {

    private final SpringDataEvaluationRepository repository;
    private final EvaluationMapper mapper;

    public EvaluationRepositoryAdapter(SpringDataEvaluationRepository repository, EvaluationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Evaluation save(Evaluation evaluation) {
        return mapper.toDomain(repository.save(mapper.toEntity(evaluation)));
    }

    @Override
    public Optional<Evaluation> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<EvaluationView> findViewById(Long id) {
        return repository.findViewById(id);
    }

    @Override
    public PageResult<EvaluationView> findViewsByTeachingPeriodId(Long teachingPeriodId, Long categoryId, PageQuery page) {
        return PageMapper.toResult(repository.findViews(teachingPeriodId, categoryId, PageMapper.pageable(page)), v -> v);
    }

    @Override
    public List<Evaluation> findByTeachingPeriodId(Long teachingPeriodId) {
        return repository.findByTeachingPeriodIdOrderByEvaluationDateAscIdAsc(teachingPeriodId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
