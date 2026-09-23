package com.edusistem.core.student.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Matrícula de un estudiante en un grupo; un estudiante puede retirarse y reingresar. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGroup {
    private Long studentId;
    private Long groupId;
    private LocalDateTime enrolledAt;
    private LocalDateTime withdrawnAt;
    private boolean active;

    public void withdraw(LocalDateTime now) {
        this.active = false;
        this.withdrawnAt = now;
    }

    public void reenroll(LocalDateTime now) {
        this.active = true;
        this.enrolledAt = now;
        this.withdrawnAt = null;
    }
}
