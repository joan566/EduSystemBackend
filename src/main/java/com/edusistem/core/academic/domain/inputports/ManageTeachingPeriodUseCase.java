package com.edusistem.core.academic.domain.inputports;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface ManageTeachingPeriodUseCase {

    TeachingPeriodView create(AcademicCommands.CreateTeachingPeriod command);

    void delete(Long teacherId, Long teachingPeriodId);

    TeachingPeriodView get(Long teacherId, Long teachingPeriodId);

    PageResult<TeachingPeriodView> list(Long teacherId, Long teachingAssignmentId, Long academicPeriodId, PageQuery page);
}
