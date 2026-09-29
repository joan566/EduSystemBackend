package com.edusistem.core.gradebook.presentation.dtos;

import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.entity.GradeAttachment;
import com.edusistem.core.gradebook.domain.entity.RubricCriterion;
import com.edusistem.core.gradebook.domain.entity.StudentObservation;
import com.edusistem.core.gradebook.domain.vo.EvaluationGradeDetail;
import com.edusistem.core.gradebook.domain.vo.GradebookEntry;
import com.edusistem.core.gradebook.domain.vo.RubricCriterionScore;
import com.edusistem.core.gradebook.domain.vo.StudentGradeReport;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.vo.CategoryBreakdown;
import com.edusistem.core.student.domain.entity.Student;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class GradebookDtos {

    private GradebookDtos() {
    }

    public record StudentResponse(Long id, String studentCode, String firstName, String lastName) {

        static StudentResponse from(Student s) {
            return new StudentResponse(s.getId(), s.getStudentCode(), s.getFirstName(), s.getLastName());
        }
    }

    public record ScaleResponse(Long id, String name, BigDecimal minimumValue, BigDecimal maximumValue) {

        static ScaleResponse from(GradingScale s) {
            return s == null ? null : new ScaleResponse(s.getId(), s.getName(), s.getMinimumValue(), s.getMaximumValue());
        }
    }

    public record CategoryResponse(Long categoryId, String categoryName, BigDecimal weight, BigDecimal achievement,
                                   BigDecimal gradeOnScale, int evaluationCount) {

        static CategoryResponse from(CategoryBreakdown b) {
            return new CategoryResponse(b.categoryId(), b.categoryName(), b.weight(), b.achievement(), b.gradeOnScale(),
                    b.evaluationCount());
        }
    }

    /**
     * Una evaluación con el resultado del estudiante. {@code weight}: peso efectivo en la nota final (%);
     * {@code contribution}: puntos sobre 100 que aporta. Nulos sin configuración completa.
     */
    public record EntryResponse(Long evaluationId, String name, String description, String type, String activityType,
                                Long categoryId, String categoryName, LocalDateTime evaluationDate,
                                BigDecimal maximumScore, BigDecimal earned, boolean excluded, BigDecimal weight,
                                BigDecimal contribution, String comment, LocalDateTime gradedAt, Long activityId,
                                Long examId, Long submissionId, boolean hasRubric, boolean hasAttachment) {

        static EntryResponse from(GradebookEntry e) {
            var r = e.row();
            return new EntryResponse(r.evaluationId(), r.name(), r.description(), r.type().name(), r.activityType(),
                    r.categoryId(), e.categoryName(), r.evaluationDate(), r.maximumScore(), r.earned(), r.excluded(),
                    e.weight(), e.contribution(), r.comment(), r.gradedAt(), r.activityId(), r.examId(),
                    r.submissionId(), e.hasRubric(), e.hasAttachment());
        }
    }

    public record ObservationResponse(String text, LocalDateTime updatedAt) {

        public static ObservationResponse from(StudentObservation o) {
            return o == null ? null : new ObservationResponse(o.getText(), o.getUpdatedAt());
        }
    }

    /**
     * Notas de un estudiante en una clase. {@code score}: nota en puntos sobre 100; {@code passing}: aprobado según
     * la nota mínima (nulo si la clase no la define o no está configurada).
     */
    public record StudentGradeReportResponse(Long teachingPeriodId, StudentResponse student, ScaleResponse scale,
                                             BigDecimal passingGrade, BigDecimal totalWeight,
                                             boolean configurationComplete, BigDecimal periodGrade, BigDecimal score,
                                             Boolean passing, List<CategoryResponse> categories,
                                             List<EntryResponse> evaluations, ObservationResponse observation) {

        public static StudentGradeReportResponse from(StudentGradeReport r) {
            return new StudentGradeReportResponse(r.teachingPeriodId(), StudentResponse.from(r.student()),
                    ScaleResponse.from(r.scale()), r.passingGrade(), r.totalWeight(), r.configurationComplete(),
                    r.periodGrade(), r.score(), r.passing(), r.categories().stream().map(CategoryResponse::from).toList(),
                    r.evaluations().stream().map(EntryResponse::from).toList(),
                    ObservationResponse.from(r.observation()));
        }
    }

    public record CriterionResponse(Long id, String name, BigDecimal weight, int position, BigDecimal score) {

        static CriterionResponse from(RubricCriterion c, BigDecimal score) {
            return new CriterionResponse(c.getId(), c.getName(), c.getWeight(), c.getPosition(), score);
        }

        static CriterionResponse from(RubricCriterionScore s) {
            return from(s.criterion(), s.score());
        }
    }

    public record AttachmentResponse(String fileName, String contentType, long sizeBytes, LocalDateTime updatedAt) {

        public static AttachmentResponse from(GradeAttachment a) {
            return a == null ? null : new AttachmentResponse(a.getFileName(), a.getContentType(), a.getSizeBytes(),
                    a.getUpdatedAt());
        }
    }

    /** La nota de un estudiante en una evaluación, con la rúbrica (vacía si no hay) y el adjunto (nulo si no hay). */
    public record GradeDetailResponse(Long teachingPeriodId, StudentResponse student, ScaleResponse scale,
                                      EntryResponse evaluation, List<CriterionResponse> rubric,
                                      AttachmentResponse attachment) {

        public static GradeDetailResponse from(EvaluationGradeDetail d) {
            return new GradeDetailResponse(d.teachingPeriodId(), StudentResponse.from(d.student()),
                    ScaleResponse.from(d.scale()), EntryResponse.from(d.entry()),
                    d.rubric().stream().map(CriterionResponse::from).toList(), AttachmentResponse.from(d.attachment()));
        }
    }

    public static List<CriterionResponse> criteria(List<RubricCriterion> criteria) {
        return criteria.stream().map(c -> CriterionResponse.from(c, null)).toList();
    }

    public record CriterionRequest(Long id, @NotNull @Size(max = 150) String name, @NotNull BigDecimal weight) {
    }

    public record RubricRequest(@NotEmpty @Valid List<CriterionRequest> criteria) {

        public List<GradebookCommands.CriterionInput> toInputs() {
            return criteria.stream().map(c -> new GradebookCommands.CriterionInput(c.id(), c.name(), c.weight())).toList();
        }
    }

    public record ScoreRequest(@NotNull Long criterionId, @NotNull BigDecimal score) {
    }

    /** {@code comment} nulo conserva el comentario actual de la nota. */
    public record RubricScoresRequest(@NotEmpty @Valid List<ScoreRequest> scores, @Size(max = 500) String comment) {

        public List<GradebookCommands.ScoreInput> toInputs() {
            return scores.stream().map(s -> new GradebookCommands.ScoreInput(s.criterionId(), s.score())).toList();
        }
    }

    public record ObservationRequest(@Size(max = 1000) String text) {
    }
}
