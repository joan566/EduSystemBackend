package com.edusistem.core.activity.domain.outputports;

import com.edusistem.core.activity.domain.entity.ActivityGrade;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ActivityGradeRepositoryPort {

    List<ActivityGrade> saveAll(List<ActivityGrade> grades);

    List<ActivityGrade> findByActivityId(Long activityId);

    Optional<ActivityGrade> findByActivityIdAndStudentId(Long activityId, Long studentId);

    Optional<BigDecimal> findMaximumGrade(Long activityId);

    boolean existsByActivityId(Long activityId);
}
