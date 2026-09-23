package com.edusistem.core.shared.infrastructure.adapter;

import com.edusistem.core.shared.domain.outputports.CatalogUsagePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class CatalogUsageAdapter implements CatalogUsagePort {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean gradeUsedByOtherTeachers(Long gradeId, Long teacherId) {
        return SqlSupport.exists(em, """
                select 1 from groups g join teaching_assignments ta on ta.group_id = g.id
                where g.grade_id = :id and ta.teacher_id <> :teacher""", "id", gradeId, "teacher", teacherId);
    }

    @Override
    public boolean groupUsedByOtherTeachers(Long groupId, Long teacherId) {
        return SqlSupport.exists(em, """
                select 1 from teaching_assignments ta
                where ta.group_id = :id and ta.teacher_id <> :teacher""", "id", groupId, "teacher", teacherId);
    }

    @Override
    public boolean subjectUsedByOtherTeachers(Long subjectId, Long teacherId) {
        return SqlSupport.exists(em, """
                select 1 from teaching_assignments ta
                where ta.subject_id = :id and ta.teacher_id <> :teacher""", "id", subjectId, "teacher", teacherId);
    }

    @Override
    public boolean academicPeriodUsedByOtherTeachers(Long academicPeriodId, Long teacherId) {
        return SqlSupport.exists(em, """
                select 1 from teaching_periods tp join teaching_assignments ta on ta.id = tp.teaching_assignment_id
                where tp.academic_period_id = :id and ta.teacher_id <> :teacher""",
                "id", academicPeriodId, "teacher", teacherId);
    }
}
