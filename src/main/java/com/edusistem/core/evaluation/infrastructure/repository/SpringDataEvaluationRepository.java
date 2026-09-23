package com.edusistem.core.evaluation.infrastructure.repository;

import com.edusistem.core.evaluation.domain.vo.EvaluationView;
import com.edusistem.core.evaluation.infrastructure.entity.EvaluationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataEvaluationRepository extends JpaRepository<EvaluationEntity, Long> {

    List<EvaluationEntity> findByTeachingPeriodIdOrderByEvaluationDateAscIdAsc(Long teachingPeriodId);

    String VIEW_SELECT = """
            select new com.edusistem.core.evaluation.domain.vo.EvaluationView(e.id, e.teachingPeriodId, c.id, c.name, e.name,
                e.description, e.evaluationDate, e.maximumScore, x.id, a.id, s.id)
            from EvaluationEntity e
            join EvaluationCategoryEntity c on c.id = e.evaluationCategoryId
            left join ExamEntity x on x.evaluationId = e.id
            left join ActivityEntity a on a.evaluationId = e.id
            left join AttendanceSessionEntity s on s.evaluationId = e.id
            """;

    @Query(VIEW_SELECT + " where e.id = :id")
    Optional<EvaluationView> findViewById(@Param("id") Long id);

    @Query(value = VIEW_SELECT + """
            where e.teachingPeriodId = :teachingPeriodId and (:categoryId is null or e.evaluationCategoryId = :categoryId)
            order by e.evaluationDate desc nulls last, e.id desc""",
            countQuery = """
                    select count(e) from EvaluationEntity e
                    where e.teachingPeriodId = :teachingPeriodId
                      and (:categoryId is null or e.evaluationCategoryId = :categoryId)""")
    Page<EvaluationView> findViews(@Param("teachingPeriodId") Long teachingPeriodId,
                                   @Param("categoryId") Long categoryId, Pageable pageable);
}
