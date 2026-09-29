package com.edusistem.core.attendance.domain.outputports;

import com.edusistem.core.attendance.domain.entity.AttendanceSession;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceSessionRepositoryPort {

    AttendanceSession save(AttendanceSession session);

    Optional<AttendanceSession> findById(Long id);

    Optional<AttendanceSessionView> findViewById(Long id);

    PageResult<AttendanceSessionView> findViewsByTeachingPeriodId(Long teachingPeriodId, PageQuery page);

    /** La sesión más reciente (mayor id) de la clase en esa fecha. */
    Optional<AttendanceSessionView> findLatestViewByTeachingPeriodIdAndDate(Long teachingPeriodId, LocalDate date);

    void deleteById(Long id);
}
