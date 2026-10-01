package com.edusistem.core.imports.application.schoolsetup.resolution;

import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.imports.application.contracts.TeachingPeriodCatalog;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.List;

public class DefaultTeachingPeriodCatalog implements TeachingPeriodCatalog {

    private final ActivityRepositoryPort activities;
    private final AttendanceSessionRepositoryPort sessions;

    public DefaultTeachingPeriodCatalog(ActivityRepositoryPort activities, AttendanceSessionRepositoryPort sessions) {
        this.activities = activities;
        this.sessions = sessions;
    }

    @Override
    public List<ActivityView> activities(Long teachingPeriodId) {
        return PageResult.collectAll(page -> activities.findViewsByTeachingPeriodId(teachingPeriodId, page));
    }

    @Override
    public List<AttendanceSessionView> sessions(Long teachingPeriodId) {
        return PageResult.collectAll(page -> sessions.findViewsByTeachingPeriodId(teachingPeriodId, page));
    }
}
