package com.edusistem.core.gradebook.domain.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Archivo adjunto a la nota de un estudiante en una evaluación (p. ej. el trabajo entregado). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeAttachment {
    private Long id;
    private Long evaluationId;
    private Long studentId;
    private String fileName;
    private String contentType;
    private long sizeBytes;
    private String storagePath;
    private LocalDateTime updatedAt;
}
