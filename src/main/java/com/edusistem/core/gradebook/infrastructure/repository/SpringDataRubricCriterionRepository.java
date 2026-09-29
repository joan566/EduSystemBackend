package com.edusistem.core.gradebook.infrastructure.repository;

import com.edusistem.core.gradebook.infrastructure.entity.RubricCriterionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRubricCriterionRepository extends JpaRepository<RubricCriterionEntity, Long> {

    List<RubricCriterionEntity> findByEvaluationIdOrderByPositionAscIdAsc(Long evaluationId);
}
