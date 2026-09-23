package com.edusistem.core.student.domain.vo;

import com.edusistem.core.student.domain.entity.Student;
import java.util.List;

public record StudentDetails(Student student, List<StudentEnrollmentView> enrollments) {
}
