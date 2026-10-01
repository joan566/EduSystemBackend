package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import java.util.List;

/** Contenido ya registrado en un teaching period (todas las páginas). */
public interface TeachingPeriodCatalog {

    List<ActivityView> activities(Long teachingPeriodId);

    List<AttendanceSessionView> sessions(Long teachingPeriodId);
}
