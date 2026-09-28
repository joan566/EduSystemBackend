package com.edusistem.core.exam.application.use_case.dtos;

import java.math.BigDecimal;

public final class SubmissionCommands {

    private SubmissionCommands() {
    }

    /** {@code studentId} solo se usa cuando el QR no es legible (asignación manual) o para verificar coherencia. */
    public record Submit(Long teacherId, Long examId, byte[] image, String fileName, Long studentId, boolean replace) {
    }

    /** PDF con varias hojas escaneadas; cada estudiante se identifica solo por el QR de su hoja. */
    public record SubmitBatch(Long teacherId, Long examId, byte[] pdf, String fileName, boolean replace) {
    }

    public record UpdateAnswer(Long teacherId, Long examId, Long submissionId, int questionNumber,
                               String selectedOption, String reason) {
    }

    public record UpdateFinalGrade(Long teacherId, Long examId, Long submissionId, BigDecimal finalGrade, String reason) {
    }
}
