package com.edusistem.core.activity.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Especialización de Evaluation calificada manualmente (taller, tarea, exposición, proyecto, quiz...). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    private Long id;
    private Long evaluationId;
    private String activityType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
