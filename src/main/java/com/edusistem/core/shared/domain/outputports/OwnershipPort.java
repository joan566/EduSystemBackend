package com.edusistem.core.shared.domain.outputports;

/**
 * Consultas de pertenencia. La propiedad de los datos académicos se deriva siempre de
 * teaching_assignments.teacher_id; nunca de un identificador enviado por el cliente.
 */
public interface OwnershipPort {

    boolean ownsTeachingAssignment(Long teacherId, Long teachingAssignmentId);

    boolean ownsTeachingPeriod(Long teacherId, Long teachingPeriodId);

    boolean ownsEvaluation(Long teacherId, Long evaluationId);

    boolean ownsExam(Long teacherId, Long examId);

    boolean ownsActivity(Long teacherId, Long activityId);

    boolean ownsAttendanceSession(Long teacherId, Long attendanceSessionId);

    /** El profesor tiene alguna asignación docente sobre el grupo. */
    boolean teachesGroup(Long teacherId, Long groupId);

    /** El estudiante pertenece (o perteneció) a algún grupo en el que el profesor enseña. */
    boolean teachesStudent(Long teacherId, Long studentId);
}
