package com.edusistem.core.grading.infrastructure.adapter;

import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.infrastructure.entity.GradingConfigurationEntity;
import com.edusistem.core.grading.infrastructure.entity.GradingWeightEntity;
import com.edusistem.core.grading.infrastructure.mapper.GradingMapper;
import com.edusistem.core.grading.infrastructure.repository.SpringDataGradingConfigurationRepository;
import com.edusistem.core.grading.infrastructure.repository.SpringDataGradingWeightRepository;
import com.edusistem.core.grading.domain.outputports.GradingConfigurationRepositoryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GradingConfigurationRepositoryAdapter implements GradingConfigurationRepositoryPort {

    private final SpringDataGradingConfigurationRepository configurations;
    private final SpringDataGradingWeightRepository weights;
    private final GradingMapper mapper;

    public GradingConfigurationRepositoryAdapter(SpringDataGradingConfigurationRepository configurations,
                                                 SpringDataGradingWeightRepository weights, GradingMapper mapper) {
        this.configurations = configurations;
        this.weights = weights;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public GradingConfiguration save(GradingConfiguration configuration) {
        GradingConfigurationEntity saved = configurations.save(mapper.toEntity(configuration));
        weights.deleteByConfigurationId(saved.getId());
        List<GradingWeightEntity> toSave = new ArrayList<>();
        for (GradingWeight w : configuration.getWeights()) {
            GradingWeightEntity entity = new GradingWeightEntity();
            entity.setGradingConfigurationId(saved.getId());
            entity.setEvaluationCategoryId(w.getEvaluationCategoryId());
            entity.setWeight(w.getWeight());
            toSave.add(entity);
        }
        weights.saveAll(toSave);
        return load(saved);
    }

    @Override
    public Optional<GradingConfiguration> findByTeachingPeriodId(Long teachingPeriodId) {
        return configurations.findByTeachingPeriodId(teachingPeriodId).map(this::load);
    }

    private GradingConfiguration load(GradingConfigurationEntity entity) {
        GradingConfiguration domain = mapper.toDomain(entity);
        domain.setWeights(weights.findByGradingConfigurationIdOrderByIdAsc(entity.getId()).stream()
                .map(mapper::toDomain).toList());
        return domain;
    }
}
