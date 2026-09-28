package com.edusistem.core.exam.domain.entity;

import com.edusistem.core.exam.domain.enums.BatchPageOutcome;
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
public class SubmissionBatchPage {
    private Long id;
    private Long batchId;
    private int pageNumber;
    private BatchPageOutcome outcome;
    private String errorCode;
    private String message;
    /** Código leído del QR (aunque la página se rechace); sirve para detectar duplicados dentro del lote. */
    private String studentCode;
    /** Solo en PROCESSED, REVIEW_REQUIRED y FAILED. */
    private Long submissionId;
    private LocalDateTime createdAt;
}
