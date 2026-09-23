package com.edusistem.core.attendance.domain.outputports;

import com.edusistem.core.attendance.domain.entity.AttendanceSession;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface AttendanceSessionRepositoryPort {

    AttendanceSession save(AttendanceSession session);

    Optional<AttendanceSession> findById(Long id);

    Optional<AttendanceSessionView> findViewById(Long id);

    PageResult<AttendanceSessionView> findViewsByTeachingPeriodId(Long teachingPeriodId, PageQuery page);

    void deleteById(Long id);
}
