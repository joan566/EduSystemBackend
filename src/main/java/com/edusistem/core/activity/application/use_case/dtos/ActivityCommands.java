package com.edusistem.core.activity.application.use_case.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ActivityCommands {

    private ActivityCommands() {
    }

    public record Create(Long teacherId, Long teachingPeriodId, String name, String description,
                         LocalDateTime evaluationDate, BigDecimal maximumScore, String activityType) {
    }

    public record Update(Long teacherId, Long activityId, String name, String description,
                         LocalDateTime evaluationDate, BigDecimal maximumScore, String activityType) {
    }

    public record GradeInput(Long studentId, BigDecimal grade, String comment) {
    }

    public record RecordGrades(Long teacherId, Long activityId, List<GradeInput> grades) {
    }
}
