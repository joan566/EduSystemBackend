package com.edusistem.core.student.domain.vo;

import com.edusistem.core.student.domain.entity.Student;

/** A student in a listing with their current course; {@code currentEnrollment} is null if never enrolled. */
public record StudentListItem(Student student, StudentEnrollmentView currentEnrollment) {
}
