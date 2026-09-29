package com.edusistem.core.gradebook.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Observación del docente sobre un estudiante en una clase. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentObservation {
    private Long id;
    private Long teachingPeriodId;
    private Long studentId;
    private String text;
    private LocalDateTime updatedAt;
}
