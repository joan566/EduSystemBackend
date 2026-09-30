package com.edusistem.core.student.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private Long id;
    /** Profesor dueño; nunca se comparte con otros profesores. */
    private Long teacherId;
    private String identificationNumber;
    private String studentCode;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String fullName() {
        return lastName + " " + firstName;
    }
}
