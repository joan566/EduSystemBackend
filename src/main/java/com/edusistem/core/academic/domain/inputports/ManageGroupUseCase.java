package com.edusistem.core.academic.domain.inputports;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface ManageGroupUseCase {

    GroupView create(AcademicCommands.CreateGroup command);

    GroupView update(AcademicCommands.UpdateGroup command);

    void delete(Long actorId, Long groupId);

    GroupView get(Long groupId);

    PageResult<GroupView> search(Long gradeId, Integer academicYear, PageQuery page);
}
