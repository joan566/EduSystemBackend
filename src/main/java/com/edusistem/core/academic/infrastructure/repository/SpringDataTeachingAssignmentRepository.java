package com.edusistem.core.academic.infrastructure.repository;

import com.edusistem.core.academic.domain.vo.TeachingAssignmentView;
import com.edusistem.core.academic.infrastructure.entity.TeachingAssignmentEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTeachingAssignmentRepository extends JpaRepository<TeachingAssignmentEntity, Long> {

    Optional<TeachingAssignmentEntity> findByTeacherIdAndGroupIdAndSubjectId(Long teacherId, Long groupId, Long subjectId);

    @Query("""
            select new com.edusistem.core.academic.domain.vo.TeachingAssignmentView(ta.id, ta.groupId, g.name, gr.name,
                g.academicYear, ta.subjectId, s.name, ta.active)
            from TeachingAssignmentEntity ta
            join GroupEntity g on g.id = ta.groupId
            join GradeEntity gr on gr.id = g.gradeId
            join SubjectEntity s on s.id = ta.subjectId
            where ta.id = :id""")
    Optional<TeachingAssignmentView> findViewById(@Param("id") Long id);

    @Query(value = """
            select new com.edusistem.core.academic.domain.vo.TeachingAssignmentView(ta.id, ta.groupId, g.name, gr.name,
                g.academicYear, ta.subjectId, s.name, ta.active)
            from TeachingAssignmentEntity ta
            join GroupEntity g on g.id = ta.groupId
            join GradeEntity gr on gr.id = g.gradeId
            join SubjectEntity s on s.id = ta.subjectId
            where ta.teacherId = :teacherId
              and (:groupId is null or ta.groupId = :groupId)
              and (:subjectId is null or ta.subjectId = :subjectId)
              and (:active is null or ta.active = :active)
            order by g.academicYear desc, gr.name, g.name, s.name""",
            countQuery = """
                    select count(ta) from TeachingAssignmentEntity ta
                    where ta.teacherId = :teacherId
                      and (:groupId is null or ta.groupId = :groupId)
                      and (:subjectId is null or ta.subjectId = :subjectId)
                      and (:active is null or ta.active = :active)""")
    Page<TeachingAssignmentView> findViewsByTeacher(@Param("teacherId") Long teacherId, @Param("groupId") Long groupId,
                                                    @Param("subjectId") Long subjectId, @Param("active") Boolean active,
                                                    Pageable pageable);
}
