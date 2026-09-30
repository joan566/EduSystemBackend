package com.edusistem.core.grading.infrastructure.repository;

import com.edusistem.core.grading.infrastructure.entity.GradingScaleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataGradingScaleRepository extends JpaRepository<GradingScaleEntity, Long> {

    @Query("select s from GradingScaleEntity s where s.teacherId is null or s.teacherId = :teacherId order by s.id")
    List<GradingScaleEntity> findVisibleTo(@Param("teacherId") Long teacherId);
}
