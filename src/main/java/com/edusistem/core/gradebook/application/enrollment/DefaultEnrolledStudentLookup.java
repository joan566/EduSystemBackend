package com.edusistem.core.gradebook.application.enrollment;

import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.gradebook.application.contracts.EnrolledStudentLookup;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;

public class DefaultEnrolledStudentLookup implements EnrolledStudentLookup {

    private final TeachingPeriodRepositoryPort teachingPeriods;
    private final StudentRepositoryPort students;
    private final StudentGroupRepositoryPort studentGroups;

    public DefaultEnrolledStudentLookup(TeachingPeriodRepositoryPort teachingPeriods, StudentRepositoryPort students,
                                        StudentGroupRepositoryPort studentGroups) {
        this.teachingPeriods = teachingPeriods;
        this.students = students;
        this.studentGroups = studentGroups;
    }

    @Override
    public Student require(Long teachingPeriodId, Long studentId) {
        TeachingPeriodView period = teachingPeriods.findViewById(teachingPeriodId)
                .orElseThrow(() -> ResourceNotFoundException.of("TeachingPeriod", teachingPeriodId));
        if (studentGroups.find(studentId, period.groupId()).isEmpty()) {
            throw new ResourceNotFoundException("STUDENT_NOT_IN_CLASS",
                    "Student " + studentId + " is not enrolled in the group of teaching period " + teachingPeriodId);
        }
        return students.findById(studentId).orElseThrow(() -> ResourceNotFoundException.of("Student", studentId));
    }
}
