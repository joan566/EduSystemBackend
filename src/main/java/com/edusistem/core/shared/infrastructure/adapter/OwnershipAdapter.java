package com.edusistem.core.shared.infrastructure.adapter;

import com.edusistem.core.shared.domain.outputports.OwnershipPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class OwnershipAdapter implements OwnershipPort {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean ownsTeachingAssignment(Long teacherId, Long teachingAssignmentId) {
        return exists("""
                select 1 from teaching_assignments ta
                where ta.id = :id and ta.teacher_id = :teacher""", teacherId, teachingAssignmentId);
    }

    @Override
    public boolean ownsTeachingPeriod(Long teacherId, Long teachingPeriodId) {
        return exists("""
                select 1 from teaching_periods tp
                join teaching_assignments ta on ta.id = tp.teaching_assignment_id
                where tp.id = :id and ta.teacher_id = :teacher""", teacherId, teachingPeriodId);
    }

    @Override
    public boolean ownsEvaluation(Long teacherId, Long evaluationId) {
        return exists("""
                select 1 from evaluations e
                join teaching_periods tp on tp.id = e.teaching_period_id
                join teaching_assignments ta on ta.id = tp.teaching_assignment_id
                where e.id = :id and ta.teacher_id = :teacher""", teacherId, evaluationId);
    }

    @Override
    public boolean ownsExam(Long teacherId, Long examId) {
        return exists("""
                select 1 from exams x
                join evaluations e on e.id = x.evaluation_id
                join teaching_periods tp on tp.id = e.teaching_period_id
                join teaching_assignments ta on ta.id = tp.teaching_assignment_id
                where x.id = :id and ta.teacher_id = :teacher""", teacherId, examId);
    }

    @Override
    public boolean ownsActivity(Long teacherId, Long activityId) {
        return exists("""
                select 1 from activities a
                join evaluations e on e.id = a.evaluation_id
                join teaching_periods tp on tp.id = e.teaching_period_id
                join teaching_assignments ta on ta.id = tp.teaching_assignment_id
                where a.id = :id and ta.teacher_id = :teacher""", teacherId, activityId);
    }

    @Override
    public boolean ownsAttendanceSession(Long teacherId, Long attendanceSessionId) {
        return exists("""
                select 1 from attendance_sessions s
                join evaluations e on e.id = s.evaluation_id
                join teaching_periods tp on tp.id = e.teaching_period_id
                join teaching_assignments ta on ta.id = tp.teaching_assignment_id
                where s.id = :id and ta.teacher_id = :teacher""", teacherId, attendanceSessionId);
    }

    @Override
    public boolean teachesGroup(Long teacherId, Long groupId) {
        return exists("""
                select 1 from teaching_assignments ta
                where ta.group_id = :id and ta.teacher_id = :teacher""", teacherId, groupId);
    }

    @Override
    public boolean teachesStudent(Long teacherId, Long studentId) {
        return exists("""
                select 1 from student_groups sg
                join teaching_assignments ta on ta.group_id = sg.group_id
                where sg.student_id = :id and ta.teacher_id = :teacher""", teacherId, studentId);
    }

    private boolean exists(String sql, Long teacherId, Long id) {
        return !em.createNativeQuery(sql + " limit 1")
                .setParameter("teacher", teacherId)
                .setParameter("id", id)
                .getResultList()
                .isEmpty();
    }
}
