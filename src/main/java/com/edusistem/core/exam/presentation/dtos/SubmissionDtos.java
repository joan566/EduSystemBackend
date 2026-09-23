package com.edusistem.core.exam.presentation.dtos;

import com.edusistem.core.exam.domain.entity.ExamAnswer;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.exam.domain.vo.ExamSubmissionSummary;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
}
