package com.edusistem.core.shared.domain.outputports;

/**
 * Consultas de pertenencia. Cada dato pertenece a un único profesor (columna teacher_id propia o heredada de
 * teaching_assignments); el profesor sale siempre del token, nunca de un identificador enviado por el cliente.
 */
public interface OwnershipPort {

    boolean ownsTeachingAssignment(Long teacherId, Long teachingAssignmentId);

    boolean ownsTeachingPeriod(Long teacherId, Long teachingPeriodId);

    boolean ownsEvaluation(Long teacherId, Long evaluationId);

    boolean ownsExam(Long teacherId, Long examId);

    boolean ownsActivity(Long teacherId, Long activityId);

    boolean ownsAttendanceSession(Long teacherId, Long attendanceSessionId);

    boolean ownsGroup(Long teacherId, Long groupId);

    boolean ownsStudent(Long teacherId, Long studentId);
}
