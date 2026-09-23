package com.edusistem.core.exam.infrastructure.repository;

import com.edusistem.core.exam.domain.vo.ExamView;
import com.edusistem.core.exam.infrastructure.entity.ExamEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataExamRepository extends JpaRepository<ExamEntity, Long> {

    String VIEW_SELECT = """
            select new com.edusistem.core.exam.domain.vo.ExamView(x.id, e.id, e.teachingPeriodId, e.name, e.description,
                e.evaluationDate, e.maximumScore, x.numberOfQuestions)
            from ExamEntity x join EvaluationEntity e on e.id = x.evaluationId
            """;

    @Query(VIEW_SELECT + " where x.id = :id")
    Optional<ExamView> findViewById(@Param("id") Long id);

    @Query(value = VIEW_SELECT + """
            where e.teachingPeriodId = :teachingPeriodId
            order by e.evaluationDate desc nulls last, x.id desc""",
            countQuery = """
                    select count(x) from ExamEntity x join EvaluationEntity e on e.id = x.evaluationId
                    where e.teachingPeriodId = :teachingPeriodId""")
    Page<ExamView> findViews(@Param("teachingPeriodId") Long teachingPeriodId, Pageable pageable);
}
