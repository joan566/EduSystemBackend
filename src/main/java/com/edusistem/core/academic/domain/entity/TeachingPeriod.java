package com.edusistem.core.academic.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** TeachingAssignment + AcademicPeriod: el contexto sobre el que se configura y registra toda la evaluación. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingPeriod {
    private Long id;
    private Long teachingAssignmentId;
    private Long academicPeriodId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
