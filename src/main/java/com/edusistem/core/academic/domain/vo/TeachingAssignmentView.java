package com.edusistem.core.academic.domain.vo;

public record TeachingAssignmentView(Long id, Long groupId, String groupName, String gradeName, int academicYear,
                                     Long subjectId, String subjectName, boolean active) {
}
