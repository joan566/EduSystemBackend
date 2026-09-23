package com.edusistem.core.activity.infrastructure.repository;

import com.edusistem.core.activity.infrastructure.entity.ActivityGradeEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataActivityGradeRepository extends JpaRepository<ActivityGradeEntity, Long> {

    List<ActivityGradeEntity> findByActivityId(Long activityId);

    Optional<ActivityGradeEntity> findByActivityIdAndStudentId(Long activityId, Long studentId);

    boolean existsByActivityId(Long activityId);

    @Query("select max(g.grade) from ActivityGradeEntity g where g.activityId = :activityId")
    BigDecimal findMaximumGrade(@Param("activityId") Long activityId);
}
