package com.edusistem.core.shared.application.service;

import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.OwnershipPort;
import org.springframework.stereotype.Component;

/**
 * Verifica que un recurso pertenezca al profesor autenticado. Si no le pertenece responde como si no existiera
 * (404) para no revelar la existencia de datos de otros profesores.
 */
@Component
public class OwnershipGuard {

    private final OwnershipPort ownership;

    public OwnershipGuard(OwnershipPort ownership) {
        this.ownership = ownership;
    }

    public void requireTeachingAssignment(Long teacherId, Long id) {
        check(ownership.ownsTeachingAssignment(teacherId, id), "TeachingAssignment", id);
    }

    public void requireTeachingPeriod(Long teacherId, Long id) {
        check(ownership.ownsTeachingPeriod(teacherId, id), "TeachingPeriod", id);
    }

    public void requireEvaluation(Long teacherId, Long id) {
        check(ownership.ownsEvaluation(teacherId, id), "Evaluation", id);
    }

    public void requireExam(Long teacherId, Long id) {
        check(ownership.ownsExam(teacherId, id), "Exam", id);
    }

    public void requireActivity(Long teacherId, Long id) {
        check(ownership.ownsActivity(teacherId, id), "Activity", id);
    }

    public void requireAttendanceSession(Long teacherId, Long id) {
        check(ownership.ownsAttendanceSession(teacherId, id), "AttendanceSession", id);
    }

    public void requireGroup(Long teacherId, Long groupId) {
        check(ownership.teachesGroup(teacherId, groupId), "Group", groupId);
    }

    public void requireStudent(Long teacherId, Long studentId) {
        check(ownership.teachesStudent(teacherId, studentId), "Student", studentId);
    }

    private void check(boolean owned, String resource, Long id) {
        if (!owned) {
            throw ResourceNotFoundException.of(resource, id);
        }
    }
}
