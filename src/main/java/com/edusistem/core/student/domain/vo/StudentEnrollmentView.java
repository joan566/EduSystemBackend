package com.edusistem.core.student.domain.vo;

import java.time.LocalDateTime;

public record StudentEnrollmentView(Long groupId, String groupName, String gradeName, int academicYear,
                                    LocalDateTime enrolledAt, LocalDateTime withdrawnAt, boolean active) {
}
