package com.edusistem.core.grading.infrastructure.repository;

import com.edusistem.core.grading.infrastructure.entity.GradingConfigurationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGradingConfigurationRepository extends JpaRepository<GradingConfigurationEntity, Long> {

    Optional<GradingConfigurationEntity> findByTeachingPeriodId(Long teachingPeriodId);
}
