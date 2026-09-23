package com.edusistem.core.student.infrastructure.repository;

import com.edusistem.core.student.domain.vo.StudentEnrollmentView;
import com.edusistem.core.student.infrastructure.entity.StudentGroupEntity;
import com.edusistem.core.student.infrastructure.entity.StudentGroupId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataStudentGroupRepository extends JpaRepository<StudentGroupEntity, StudentGroupId> {

    boolean existsByStudentIdAndGroupIdAndActiveTrue(Long studentId, Long groupId);

    @Query("""
            select new com.edusistem.core.student.domain.vo.StudentEnrollmentView(g.id, g.name, gr.name, g.academicYear,
                sg.enrolledAt, sg.withdrawnAt, sg.active)
            from StudentGroupEntity sg
            join GroupEntity g on g.id = sg.groupId
            join GradeEntity gr on gr.id = g.gradeId
            where sg.studentId = :studentId
            order by g.academicYear desc, gr.name, g.name""")
    List<StudentEnrollmentView> findEnrollmentViews(@Param("studentId") Long studentId);
}
