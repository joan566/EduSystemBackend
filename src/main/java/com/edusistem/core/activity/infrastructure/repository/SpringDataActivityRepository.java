package com.edusistem.core.activity.infrastructure.repository;

import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.activity.infrastructure.entity.ActivityEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataActivityRepository extends JpaRepository<ActivityEntity, Long> {

    String VIEW_SELECT = """
            select new com.edusistem.core.activity.domain.vo.ActivityView(a.id, e.id, e.teachingPeriodId, e.name,
                e.description, e.evaluationDate, e.maximumScore, a.activityType)
            from ActivityEntity a join EvaluationEntity e on e.id = a.evaluationId
            """;

    @Query(VIEW_SELECT + " where a.id = :id")
    Optional<ActivityView> findViewById(@Param("id") Long id);

    @Query(value = VIEW_SELECT + """
            where e.teachingPeriodId = :teachingPeriodId
            order by e.evaluationDate desc nulls last, a.id desc""",
            countQuery = """
                    select count(a) from ActivityEntity a join EvaluationEntity e on e.id = a.evaluationId
                    where e.teachingPeriodId = :teachingPeriodId""")
    Page<ActivityView> findViews(@Param("teachingPeriodId") Long teachingPeriodId, Pageable pageable);
}
