package com.edusistem.core.gradebook.infrastructure.repository;

import com.edusistem.core.gradebook.infrastructure.entity.RubricScoreEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricScoreRepository extends JpaRepository<RubricScoreEntity, Long> {

    @Query("""
            select s from RubricScoreEntity s, RubricCriterionEntity c
            where c.id = s.criterionId and c.evaluationId = :evaluationId and s.studentId = :studentId""")
    List<RubricScoreEntity> findByEvaluationAndStudent(@Param("evaluationId") Long evaluationId,
                                                       @Param("studentId") Long studentId);
}
