package com.edusistem.core.grading.presentation.dtos;

import com.edusistem.core.grading.application.use_case.dtos.GradingCommands;
import com.edusistem.core.grading.domain.entity.GradingScale;
import com.edusistem.core.grading.domain.vo.CategoryBreakdown;
import com.edusistem.core.grading.domain.vo.GradingConfigurationView;
import com.edusistem.core.grading.domain.vo.PeriodGradeReport;
import com.edusistem.core.grading.domain.vo.StudentPeriodGrade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class GradingDtos {

    private GradingDtos() {
    }

    public record ScaleRequest(@NotBlank @Size(max = 100) String name, @NotNull BigDecimal minimumValue,
                               @NotNull BigDecimal maximumValue) {
    }

    public record ScaleResponse(Long id, String name, BigDecimal minimumValue, BigDecimal maximumValue) {

        public static ScaleResponse from(GradingScale s) {
            return new ScaleResponse(s.getId(), s.getName(), s.getMinimumValue(), s.getMaximumValue());
        }
    }

    public record WeightRequest(@NotNull Long evaluationCategoryId,
                                @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal weight) {
    }

    public record ConfigurationRequest(@NotNull Long gradingScaleId, @NotEmpty @Valid List<WeightRequest> weights,
                                       BigDecimal passingGrade) {

        public List<GradingCommands.WeightInput> toWeights() {
            return weights.stream().map(w -> new GradingCommands.WeightInput(w.evaluationCategoryId(), w.weight())).toList();
        }
    }

    public record WeightResponse(Long evaluationCategoryId, BigDecimal weight) {
    }

    public record ConfigurationResponse(Long id, Long teachingPeriodId, ScaleResponse scale, List<WeightResponse> weights,
                                        BigDecimal totalWeight, boolean complete, BigDecimal passingGrade) {

        public static ConfigurationResponse from(GradingConfigurationView view) {
            var c = view.configuration();
            return new ConfigurationResponse(c.getId(), c.getTeachingPeriodId(), ScaleResponse.from(view.scale()),
                    c.getWeights().stream().map(w -> new WeightResponse(w.getEvaluationCategoryId(), w.getWeight())).toList(),
                    c.totalWeight(), c.isComplete(), c.getPassingGrade());
        }
    }

    public record CategoryBreakdownResponse(Long categoryId, String categoryName, BigDecimal weight,
                                            BigDecimal achievement, BigDecimal gradeOnScale, int evaluationCount) {

        static CategoryBreakdownResponse from(CategoryBreakdown b) {
            return new CategoryBreakdownResponse(b.categoryId(), b.categoryName(), b.weight(), b.achievement(),
                    b.gradeOnScale(), b.evaluationCount());
        }
    }

    /** {@code passing}: aprobado/reprobado según la nota mínima; nulo si la clase no la define. */
    public record StudentPeriodGradeResponse(Long studentId, String studentCode, String studentName,
                                             List<CategoryBreakdownResponse> categories, BigDecimal periodGrade,
                                             Boolean passing) {

        static StudentPeriodGradeResponse from(StudentPeriodGrade g, BigDecimal passingGrade) {
            return new StudentPeriodGradeResponse(g.studentId(), g.studentCode(), g.studentName(),
                    g.categories().stream().map(CategoryBreakdownResponse::from).toList(), g.periodGrade(),
                    passingGrade == null || g.periodGrade() == null ? null
                            : g.periodGrade().compareTo(passingGrade) >= 0);
        }
    }

    public record PeriodGradeResponse(Long teachingPeriodId, ScaleResponse scale, BigDecimal passingGrade,
                                      List<StudentPeriodGradeResponse> students) {

        public static PeriodGradeResponse from(PeriodGradeReport r) {
            return new PeriodGradeResponse(r.teachingPeriodId(), ScaleResponse.from(r.scale()), r.passingGrade(),
                    r.students().stream().map(g -> StudentPeriodGradeResponse.from(g, r.passingGrade())).toList());
        }
    }
}
