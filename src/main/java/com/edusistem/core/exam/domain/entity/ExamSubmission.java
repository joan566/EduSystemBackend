package com.edusistem.core.exam.domain.entity;

import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Este estudiante presentó este examen. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmission {
    private Long id;
    private Long examId;
    private Long studentId;
    private String studentCode;
    private String detectedQrData;
    private String imagePath;
    private ExamSubmissionStatus status;
    /** Puntuación bruta (suma de puntos de respuestas correctas). */
    private BigDecimal score;
    /** Nota convertida a la escala de calificación. */
    private BigDecimal finalGrade;
    private String statusDetail;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
    @Builder.Default
    private List<ExamAnswer> answers = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean needsReview() {
        return answers.stream().anyMatch(ExamAnswer::needsReview);
    }

    public void markFailed(String detail, LocalDateTime now) {
        this.status = ExamSubmissionStatus.FAILED;
        this.statusDetail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 500));
        this.processedAt = now;
        this.score = null;
        this.finalGrade = null;
        this.answers = new ArrayList<>();
    }
}
