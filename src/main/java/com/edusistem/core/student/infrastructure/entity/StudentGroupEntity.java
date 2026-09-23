package com.edusistem.core.student.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "student_groups")
@IdClass(StudentGroupId.class)
public class StudentGroupEntity {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(nullable = false)
    private boolean active;
}
