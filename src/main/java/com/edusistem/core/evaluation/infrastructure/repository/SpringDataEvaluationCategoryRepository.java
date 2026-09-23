package com.edusistem.core.evaluation.infrastructure.repository;

import com.edusistem.core.evaluation.infrastructure.entity.EvaluationCategoryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataEvaluationCategoryRepository extends JpaRepository<EvaluationCategoryEntity, Long> {

    Optional<EvaluationCategoryEntity> findByName(String name);
}
