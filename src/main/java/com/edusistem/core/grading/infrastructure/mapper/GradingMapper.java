package com.edusistem.core.grading.infrastructure.mapper;

import com.edusistem.core.grading.domain.entity.GradingConfiguration;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.entity.GradingWeight;
import com.edusistem.core.grading.infrastructure.entity.GradingConfigurationEntity;
import com.edusistem.core.grading.infrastructure.entity.GradingScaleEntity;
import com.edusistem.core.grading.infrastructure.entity.GradingWeightEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GradingMapper {

    GradingScale toDomain(GradingScaleEntity entity);

    GradingScaleEntity toEntity(GradingScale scale);

    @Mapping(target = "weights", ignore = true)
    GradingConfiguration toDomain(GradingConfigurationEntity entity);

    GradingConfigurationEntity toEntity(GradingConfiguration configuration);

    GradingWeight toDomain(GradingWeightEntity entity);
}
