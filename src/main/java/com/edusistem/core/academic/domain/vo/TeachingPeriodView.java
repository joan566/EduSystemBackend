package com.edusistem.core.academic.domain.vo;

import java.time.LocalDate;

public record TeachingPeriodView(Long id, Long teachingAssignmentId, Long groupId, String groupName, String gradeName,
                                 int academicYear, Long subjectId, String subjectName, Long academicPeriodId,
                                 String academicPeriodName, LocalDate startDate, LocalDate endDate) {
}
