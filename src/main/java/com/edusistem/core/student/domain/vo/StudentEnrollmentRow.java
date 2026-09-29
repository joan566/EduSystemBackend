package com.edusistem.core.student.domain.vo;

import java.time.LocalDateTime;

/** A {@link StudentEnrollmentView} tagged with its student, for loading many students' enrollments at once. */
public record StudentEnrollmentRow(Long studentId, Long groupId, String groupName, String gradeName, int academicYear,
                                   LocalDateTime enrolledAt, LocalDateTime withdrawnAt, boolean active) {

    public StudentEnrollmentView toView() {
        return new StudentEnrollmentView(groupId, groupName, gradeName, academicYear, enrolledAt, withdrawnAt, active);
    }
}
