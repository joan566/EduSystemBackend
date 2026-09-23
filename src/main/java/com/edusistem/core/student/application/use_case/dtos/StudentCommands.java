package com.edusistem.core.student.application.use_case.dtos;

public final class StudentCommands {

    private StudentCommands() {
    }

    /** {@code studentCode} es opcional: si es nulo se genera uno. */
    public record Create(String identificationNumber, String studentCode, String firstName, String lastName,
                         String email) {
    }

    public record Update(Long studentId, String firstName, String lastName, String email) {
    }

    public record Enroll(Long studentId, Long groupId) {
    }

    public record Withdraw(Long teacherId, Long studentId, Long groupId) {
    }
}
