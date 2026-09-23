package com.edusistem.core.activity.presentation.dtos;

import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.activity.domain.vo.StudentGradeView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record CreateActivityRequest(@NotNull Long teachingPeriodId, @NotBlank @Size(max = 150) String name,
                                        String description, LocalDateTime evaluationDate,
                                        @NotNull @Positive BigDecimal maximumScore, @Size(max = 50) String activityType) {
    }

    public record UpdateActivityRequest(@NotBlank @Size(max = 150) String name, String description,
                                        LocalDateTime evaluationDate, @Positive BigDecimal maximumScore,
                                        @Size(max = 50) String activityType) {
    }

    public record ActivityResponse(Long id, Long evaluationId, Long teachingPeriodId, String name, String description,
                                   LocalDateTime evaluationDate, BigDecimal maximumScore, String activityType) {

        public static ActivityResponse from(ActivityView v) {
            return new ActivityResponse(v.activityId(), v.evaluationId(), v.teachingPeriodId(), v.name(),
                    v.description(), v.evaluationDate(), v.maximumScore(), v.activityType());
        }
    }

    public record GradeRequest(@NotNull Long studentId, @NotNull @DecimalMin("0.00") BigDecimal grade,
                               @Size(max = 500) String comment) {
    }

    public record SingleGradeRequest(@NotNull @DecimalMin("0.00") BigDecimal grade, @Size(max = 500) String comment) {
    }

    public record RecordGradesRequest(@NotEmpty @Valid List<GradeRequest> grades) {
    }

    public record StudentGradeResponse(Long studentId, String studentCode, String studentName, BigDecimal grade,
                                       String comment, LocalDateTime gradedAt) {

        public static StudentGradeResponse from(StudentGradeView v) {
            return new StudentGradeResponse(v.studentId(), v.studentCode(), v.studentName(), v.grade(), v.comment(),
                    v.gradedAt());
        }
    }
}
