package com.edusistem.core.evaluation.infrastructure.mapper;

import com.edusistem.core.evaluation.domain.entity.Evaluation;
import com.edusistem.core.evaluation.domain.entity.EvaluationCategory;
import com.edusistem.core.evaluation.infrastructure.entity.EvaluationCategoryEntity;
import com.edusistem.core.evaluation.infrastructure.entity.EvaluationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvaluationMapper {

    Evaluation toDomain(EvaluationEntity entity);

    EvaluationEntity toEntity(Evaluation evaluation);

    EvaluationCategory toDomain(EvaluationCategoryEntity entity);
}
