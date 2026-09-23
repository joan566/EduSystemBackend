package com.edusistem.core.academic.presentation.dtos;

import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.entity.Grade;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.academic.domain.vo.TeachingAssignmentView;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class AcademicDtos {

    private AcademicDtos() {
    }

    public record GradeRequest(@NotBlank @Size(max = 50) String name, @Size(max = 255) String description) {
    }

    public record GradeResponse(Long id, String name, String description) {

        public static GradeResponse from(Grade grade) {
            return new GradeResponse(grade.getId(), grade.getName(), grade.getDescription());
        }
    }

    public record CreateGroupRequest(@NotNull Long gradeId, @NotBlank @Size(max = 50) String name,
                                     @NotNull @Min(2000) @Max(2200) Integer academicYear) {
    }

    public record UpdateGroupRequest(@NotBlank @Size(max = 50) String name,
                                     @NotNull @Min(2000) @Max(2200) Integer academicYear) {
    }

    public record GroupResponse(Long id, Long gradeId, String gradeName, String name, int academicYear) {

        public static GroupResponse from(GroupView view) {
            return new GroupResponse(view.id(), view.gradeId(), view.gradeName(), view.name(), view.academicYear());
        }
    }

    public record AcademicPeriodRequest(@NotBlank @Size(max = 100) String name, @NotNull LocalDate startDate,
                                        @NotNull LocalDate endDate) {
    }

    public record AcademicPeriodResponse(Long id, String name, LocalDate startDate, LocalDate endDate) {

        public static AcademicPeriodResponse from(AcademicPeriod period) {
            return new AcademicPeriodResponse(period.getId(), period.getName(), period.getStartDate(), period.getEndDate());
        }
    }

    public record TeachingAssignmentRequest(@NotNull Long groupId, @NotNull Long subjectId) {
    }

    public record ActiveRequest(@NotNull Boolean active) {
    }

    public record TeachingAssignmentResponse(Long id, Long groupId, String groupName, String gradeName, int academicYear,
                                             Long subjectId, String subjectName, boolean active) {

        public static TeachingAssignmentResponse from(TeachingAssignmentView v) {
            return new TeachingAssignmentResponse(v.id(), v.groupId(), v.groupName(), v.gradeName(), v.academicYear(),
                    v.subjectId(), v.subjectName(), v.active());
        }
    }

    public record TeachingPeriodRequest(@NotNull Long teachingAssignmentId, @NotNull Long academicPeriodId) {
    }

    public record TeachingPeriodResponse(Long id, Long teachingAssignmentId, Long groupId, String groupName,
                                         String gradeName, int academicYear, Long subjectId, String subjectName,
                                         Long academicPeriodId, String academicPeriodName, LocalDate startDate,
                                         LocalDate endDate) {

        public static TeachingPeriodResponse from(TeachingPeriodView v) {
            return new TeachingPeriodResponse(v.id(), v.teachingAssignmentId(), v.groupId(), v.groupName(),
                    v.gradeName(), v.academicYear(), v.subjectId(), v.subjectName(), v.academicPeriodId(),
                    v.academicPeriodName(), v.startDate(), v.endDate());
        }
    }
}
