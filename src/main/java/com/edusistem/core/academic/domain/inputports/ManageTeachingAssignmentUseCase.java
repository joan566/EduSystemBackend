package com.edusistem.core.academic.domain.inputports;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.vo.TeachingAssignmentView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface ManageTeachingAssignmentUseCase {

    TeachingAssignmentView create(AcademicCommands.CreateTeachingAssignment command);

    TeachingAssignmentView setActive(Long teacherId, Long assignmentId, boolean active);

    void delete(Long teacherId, Long assignmentId);

    TeachingAssignmentView get(Long teacherId, Long assignmentId);

    PageResult<TeachingAssignmentView> list(Long teacherId, Long groupId, Long subjectId, Boolean active, PageQuery page);
}
