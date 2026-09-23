package com.edusistem.core.grading.infrastructure.repository;

import com.edusistem.core.grading.infrastructure.entity.GradingScaleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGradingScaleRepository extends JpaRepository<GradingScaleEntity, Long> {
}
