package com.edusistem.core.academic.infrastructure.repository;

import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.academic.infrastructure.entity.GroupEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataGroupRepository extends JpaRepository<GroupEntity, Long> {

    Optional<GroupEntity> findByGradeIdAndNameAndAcademicYear(Long gradeId, String name, int academicYear);

    @Query("""
            select g from GroupEntity g join GradeEntity gr on gr.id = g.gradeId
            where gr.name = :gradeName and g.name = :name and g.academicYear = :year""")
    Optional<GroupEntity> findByGradeNameAndName(@Param("gradeName") String gradeName, @Param("name") String name,
                                                 @Param("year") int year);

    @Query("""
            select new com.edusistem.core.academic.domain.vo.GroupView(g.id, g.gradeId, gr.name, g.name, g.academicYear)
            from GroupEntity g join GradeEntity gr on gr.id = g.gradeId where g.id = :id""")
    Optional<GroupView> findViewById(@Param("id") Long id);

    @Query(value = """
            select new com.edusistem.core.academic.domain.vo.GroupView(g.id, g.gradeId, gr.name, g.name, g.academicYear)
            from GroupEntity g join GradeEntity gr on gr.id = g.gradeId
            where (:gradeId is null or g.gradeId = :gradeId) and (:year is null or g.academicYear = :year)
            order by g.academicYear desc, gr.name, g.name""",
            countQuery = """
                    select count(g) from GroupEntity g
                    where (:gradeId is null or g.gradeId = :gradeId) and (:year is null or g.academicYear = :year)""")
    Page<GroupView> searchViews(@Param("gradeId") Long gradeId, @Param("year") Integer year, Pageable pageable);
}
