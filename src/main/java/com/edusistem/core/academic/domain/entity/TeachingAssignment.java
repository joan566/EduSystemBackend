package com.edusistem.core.academic.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Profesor + Grupo + Asignatura. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingAssignment {
    private Long id;
    private Long teacherId;
    private Long groupId;
    private Long subjectId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
