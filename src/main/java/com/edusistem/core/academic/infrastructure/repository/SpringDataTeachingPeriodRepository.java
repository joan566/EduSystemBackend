package com.edusistem.core.academic.infrastructure.repository;

import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.academic.infrastructure.entity.TeachingPeriodEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTeachingPeriodRepository extends JpaRepository<TeachingPeriodEntity, Long> {

    Optional<TeachingPeriodEntity> findByTeachingAssignmentIdAndAcademicPeriodId(Long teachingAssignmentId,
                                                                                 Long academicPeriodId);

    String VIEW_SELECT = """
            select new com.edusistem.core.academic.domain.vo.TeachingPeriodView(tp.id, tp.teachingAssignmentId, ta.groupId,
                g.name, gr.name, g.academicYear, ta.subjectId, s.name, tp.academicPeriodId, ap.name, ap.startDate, ap.endDate)
            from TeachingPeriodEntity tp
            join TeachingAssignmentEntity ta on ta.id = tp.teachingAssignmentId
            join GroupEntity g on g.id = ta.groupId
            join GradeEntity gr on gr.id = g.gradeId
            join SubjectEntity s on s.id = ta.subjectId
            join AcademicPeriodEntity ap on ap.id = tp.academicPeriodId
            """;

    @Query(VIEW_SELECT + " where tp.id = :id")
    Optional<TeachingPeriodView> findViewById(@Param("id") Long id);

    @Query(value = VIEW_SELECT + """
            where ta.teacherId = :teacherId
              and (:assignmentId is null or tp.teachingAssignmentId = :assignmentId)
              and (:periodId is null or tp.academicPeriodId = :periodId)
            order by ap.startDate desc, g.name, s.name""",
            countQuery = """
                    select count(tp) from TeachingPeriodEntity tp
                    join TeachingAssignmentEntity ta on ta.id = tp.teachingAssignmentId
                    where ta.teacherId = :teacherId
                      and (:assignmentId is null or tp.teachingAssignmentId = :assignmentId)
                      and (:periodId is null or tp.academicPeriodId = :periodId)""")
    Page<TeachingPeriodView> findViewsByTeacher(@Param("teacherId") Long teacherId,
                                                @Param("assignmentId") Long assignmentId,
                                                @Param("periodId") Long periodId, Pageable pageable);
}
