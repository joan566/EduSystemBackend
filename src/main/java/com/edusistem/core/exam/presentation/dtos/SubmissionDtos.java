package com.edusistem.core.exam.presentation.dtos;

import com.edusistem.core.exam.domain.entity.ExamAnswer;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.exam.domain.entity.SubmissionBatch;
import com.edusistem.core.exam.domain.entity.SubmissionBatchPage;
import com.edusistem.core.exam.domain.enums.BatchPageOutcome;
import com.edusistem.core.exam.domain.vo.ExamSubmissionSummary;
import com.edusistem.core.exam.domain.vo.SubmissionBatchDetails;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class SubmissionDtos {

    private SubmissionDtos() {
    }

    /** {@code selectedOption} nulo o vacío confirma la pregunta como sin respuesta. */
    public record UpdateAnswerRequest(@Pattern(regexp = "[A-Za-z]?", message = "must be a single letter or empty") String selectedOption,
                                      @Size(max = 300) String reason) {
    }

    public record UpdateFinalGradeRequest(@NotNull @DecimalMin("0.00") BigDecimal finalGrade, @Size(max = 300) String reason) {
    }

    public record SubmissionSummaryResponse(Long id, Long studentId, String studentCode, String studentName,
                                            String status, BigDecimal score, BigDecimal finalGrade, String statusDetail,
                                            LocalDateTime submittedAt, LocalDateTime processedAt) {

        public static SubmissionSummaryResponse from(ExamSubmissionSummary s) {
            return new SubmissionSummaryResponse(s.id(), s.studentId(), s.studentCode(), s.studentName(),
                    s.status().name(), s.score(), s.finalGrade(), s.statusDetail(), s.submittedAt(), s.processedAt());
        }
    }

    public record AnswerResponse(int questionNumber, String statement, String selectedOption, String correctOption,
                                 Boolean correct, String detectionStatus, BigDecimal detectionConfidence,
                                 BigDecimal points, boolean needsReview) {
    }

    public record StudentSummary(Long id, String studentCode, String name) {
    }

    public record SubmissionResponse(Long id, Long examId, String examName, StudentSummary student, String status,
                                     String statusDetail, BigDecimal score, BigDecimal maximumScore,
                                     BigDecimal finalGrade, BigDecimal scaleMinimum, BigDecimal scaleMaximum,
                                     LocalDateTime submittedAt, LocalDateTime processedAt, boolean hasImage,
                                     List<AnswerResponse> answers) {

        public static SubmissionResponse from(SubmissionDetails d) {
            ExamSubmission s = d.submission();
            List<AnswerResponse> answers = s.getAnswers().stream().map(a -> toAnswer(d, a))
                    .sorted(java.util.Comparator.comparingInt(AnswerResponse::questionNumber)).toList();
            return new SubmissionResponse(s.getId(), s.getExamId(), d.examName(),
                    new StudentSummary(d.student().getId(), d.student().getStudentCode(), d.student().fullName()),
                    s.getStatus().name(), s.getStatusDetail(), s.getScore(), d.maximumScore(), s.getFinalGrade(),
                    d.scaleMinimum(), d.scaleMaximum(), s.getSubmittedAt(), s.getProcessedAt(), s.getImagePath() != null,
                    answers);
        }

        private static AnswerResponse toAnswer(SubmissionDetails d, ExamAnswer a) {
            ExamQuestion q = d.exam().getQuestions().stream().filter(x -> x.getId().equals(a.getQuestionId()))
                    .findFirst().orElseThrow();
            return new AnswerResponse(q.getQuestionNumber(), q.getStatement(), a.getSelectedOption(),
                    q.getCorrectOption(), a.getCorrect(), a.getDetectionStatus().name(), a.getDetectionConfidence(),
                    q.getPoints(), a.needsReview());
        }
    }

    /** Estado y progreso de un lote, sin el detalle por página. */
    public record SubmissionBatchSummaryResponse(Long id, Long examId, String fileName, String status,
                                                 String statusDetail, int totalPages, int processedPages,
                                                 int progressPercent, boolean replaceExisting,
                                                 LocalDateTime createdAt, LocalDateTime startedAt,
                                                 LocalDateTime completedAt, LocalDateTime filePurgedAt) {

        public static SubmissionBatchSummaryResponse from(SubmissionBatch b) {
            int percent = b.getTotalPages() == 0 ? 0 : b.getProcessedPages() * 100 / b.getTotalPages();
            return new SubmissionBatchSummaryResponse(b.getId(), b.getExamId(), b.getFileName(), b.getStatus().name(),
                    b.getStatusDetail(), b.getTotalPages(), b.getProcessedPages(), percent, b.isReplaceExisting(),
                    b.getCreatedAt(), b.getStartedAt(), b.getCompletedAt(), b.getFilePurgedAt());
        }
    }

    /** {@code submission} es nulo en páginas REJECTED y SKIPPED. */
    public record BatchPageResponse(int page, String outcome, String errorCode, String message,
                                    SubmissionSummaryResponse submission) {
    }

    /** Conteos por resultado; promedios y extremos sobre las hojas con nota (REVIEW_REQUIRED cuenta como provisional). */
    public record BatchResultsResponse(int processed, int reviewRequired, int failed, int rejected, int skipped,
                                       int graded, BigDecimal averageScore, BigDecimal averageFinalGrade,
                                       BigDecimal highestFinalGrade, BigDecimal lowestFinalGrade) {
    }

    public record SubmissionBatchResponse(SubmissionBatchSummaryResponse batch, BatchResultsResponse results,
                                          List<BatchPageResponse> pages) {

        public static SubmissionBatchResponse from(SubmissionBatchDetails d) {
            List<BatchPageResponse> pages = d.pages().stream().map(p -> {
                ExamSubmissionSummary s = p.getSubmissionId() == null ? null : d.submissionsById().get(p.getSubmissionId());
                return new BatchPageResponse(p.getPageNumber(), p.getOutcome().name(), p.getErrorCode(), p.getMessage(),
                        s == null ? null : SubmissionSummaryResponse.from(s));
            }).toList();
            List<ExamSubmissionSummary> graded = pages.stream().map(BatchPageResponse::submission).filter(Objects::nonNull)
                    .map(r -> d.submissionsById().get(r.id())).filter(s -> s.finalGrade() != null).toList();
            List<BigDecimal> grades = graded.stream().map(ExamSubmissionSummary::finalGrade).toList();
            BatchResultsResponse results = new BatchResultsResponse(count(d, BatchPageOutcome.PROCESSED),
                    count(d, BatchPageOutcome.REVIEW_REQUIRED), count(d, BatchPageOutcome.FAILED),
                    count(d, BatchPageOutcome.REJECTED), count(d, BatchPageOutcome.SKIPPED), graded.size(),
                    average(graded.stream().map(ExamSubmissionSummary::score).filter(Objects::nonNull).toList()),
                    average(grades), grades.stream().max(BigDecimal::compareTo).orElse(null),
                    grades.stream().min(BigDecimal::compareTo).orElse(null));
            return new SubmissionBatchResponse(SubmissionBatchSummaryResponse.from(d.batch()), results, pages);
        }

        private static int count(SubmissionBatchDetails d, BatchPageOutcome outcome) {
            return (int) d.pages().stream().map(SubmissionBatchPage::getOutcome).filter(o -> o == outcome).count();
        }

        private static BigDecimal average(List<BigDecimal> values) {
            if (values.isEmpty()) {
                return null;
            }
            return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        }
    }
}
