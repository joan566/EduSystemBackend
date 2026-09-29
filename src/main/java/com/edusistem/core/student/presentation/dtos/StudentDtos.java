package com.edusistem.core.student.presentation.dtos;

import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.vo.StudentDetails;
import com.edusistem.core.student.domain.vo.StudentEnrollmentView;
import com.edusistem.core.student.domain.vo.StudentListItem;
import java.time.LocalDateTime;
import java.util.List;

public final class StudentDtos {

    private StudentDtos() {
    }

    public record StudentResponse(Long id, String studentCode, String identificationNumber, String firstName,
                                  String lastName, String email) {

        public static StudentResponse from(Student s) {
            return new StudentResponse(s.getId(), s.getStudentCode(), s.getIdentificationNumber(), s.getFirstName(),
                    s.getLastName(), s.getEmail());
        }
    }

    public record EnrollmentResponse(Long groupId, String groupName, String gradeName, int academicYear,
                                     LocalDateTime enrolledAt, LocalDateTime withdrawnAt, boolean active) {

        static EnrollmentResponse from(StudentEnrollmentView v) {
            return new EnrollmentResponse(v.groupId(), v.groupName(), v.gradeName(), v.academicYear(), v.enrolledAt(),
                    v.withdrawnAt(), v.active());
        }
    }

    /** Listing row: the student plus their current course ({@code currentEnrollment} null if never enrolled). */
    public record StudentListResponse(Long id, String studentCode, String identificationNumber, String firstName,
                                      String lastName, String email, EnrollmentResponse currentEnrollment) {

        public static StudentListResponse from(StudentListItem item) {
            Student s = item.student();
            return new StudentListResponse(s.getId(), s.getStudentCode(), s.getIdentificationNumber(), s.getFirstName(),
                    s.getLastName(), s.getEmail(),
                    item.currentEnrollment() == null ? null : EnrollmentResponse.from(item.currentEnrollment()));
        }
    }

    public record StudentDetailResponse(StudentResponse student, List<EnrollmentResponse> enrollments) {

        public static StudentDetailResponse from(StudentDetails details) {
            return new StudentDetailResponse(StudentResponse.from(details.student()),
                    details.enrollments().stream().map(EnrollmentResponse::from).toList());
        }
    }
}
