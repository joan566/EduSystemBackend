package com.edusistem.core.academic.domain.inputports;

import com.edusistem.core.academic.application.use_case.dtos.AcademicCommands;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import java.util.List;

public interface ManageTeachingPeriodScheduleUseCase {

    List<ScheduledClassView> list(Long teacherId, Long teachingPeriodId);

    ScheduledClassView create(AcademicCommands.SaveSchedule command);

    ScheduledClassView update(AcademicCommands.SaveSchedule command);

    void delete(Long teacherId, Long teachingPeriodId, Long scheduleId);
}
