package com.edusistem.core.gradebook.application.contracts;

import com.edusistem.core.student.domain.entity.Student;

/** Estudiante de una clase: debe estar (o haber estado) matriculado en el grupo del teaching period. */
public interface EnrolledStudentLookup {

    /**
     * @throws com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException si el teaching period o el
     *         estudiante no existen, o si el estudiante nunca estuvo matriculado en el grupo
     */
    Student require(Long teachingPeriodId, Long studentId);
}
