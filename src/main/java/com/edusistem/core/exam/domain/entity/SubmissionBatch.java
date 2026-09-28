package com.edusistem.core.exam.domain.entity;

import com.edusistem.core.exam.domain.enums.SubmissionBatchStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un PDF con varias hojas escaneadas que se califica en segundo plano, página a página. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionBatch {
    private Long id;
    private Long examId;
    private Long teacherId;
    private String fileName;
    /** Ruta del PDF original en el almacenamiento; permite reanudar tras un reinicio. Nula una vez purgado. */
    private String filePath;
    /** Cuándo se borró el PDF original por el periodo de retención. */
    private LocalDateTime filePurgedAt;
    private boolean replaceExisting;
    private SubmissionBatchStatus status;
    private String statusDetail;
    private int totalPages;
    /** Páginas ya resueltas (en orden); el procesamiento se reanuda desde la siguiente. */
    private int processedPages;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isFinished() {
        return status == SubmissionBatchStatus.COMPLETED || status == SubmissionBatchStatus.FAILED;
    }

    public void start(LocalDateTime now) {
        status = SubmissionBatchStatus.PROCESSING;
        if (startedAt == null) {
            startedAt = now;
        }
    }

    public void pageProcessed() {
        processedPages++;
    }

    public void complete(LocalDateTime now) {
        status = SubmissionBatchStatus.COMPLETED;
        completedAt = now;
    }

    public void filePurged(LocalDateTime now) {
        filePath = null;
        filePurgedAt = now;
    }

    public void fail(String detail, LocalDateTime now) {
        status = SubmissionBatchStatus.FAILED;
        statusDetail = detail;
        completedAt = now;
    }
}
