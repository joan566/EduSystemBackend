package com.edusistem.core.activity.domain.inputports;

import com.edusistem.core.activity.application.use_case.dtos.ActivityCommands;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface ManageActivityUseCase {

    ActivityView create(ActivityCommands.Create command);

    ActivityView get(Long teacherId, Long activityId);

    ActivityView update(ActivityCommands.Update command);

    /** Solo si aún no tiene notas registradas. */
    void delete(Long teacherId, Long activityId);

    PageResult<ActivityView> search(Long teacherId, Long teachingPeriodId, PageQuery page);
}
