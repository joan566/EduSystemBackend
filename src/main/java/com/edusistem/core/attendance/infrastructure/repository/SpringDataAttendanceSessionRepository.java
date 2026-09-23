package com.edusistem.core.attendance.infrastructure.repository;

import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.attendance.infrastructure.entity.AttendanceSessionEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataAttendanceSessionRepository extends JpaRepository<AttendanceSessionEntity, Long> {

    String VIEW_SELECT = """
            select new com.edusistem.core.attendance.domain.vo.AttendanceSessionView(s.id, e.id, e.teachingPeriodId,
                e.name, s.sessionDate, e.maximumScore)
            from AttendanceSessionEntity s join EvaluationEntity e on e.id = s.evaluationId
            """;

    @Query(VIEW_SELECT + " where s.id = :id")
    Optional<AttendanceSessionView> findViewById(@Param("id") Long id);

    @Query(value = VIEW_SELECT + """
            where e.teachingPeriodId = :teachingPeriodId
            order by s.sessionDate desc, s.id desc""",
            countQuery = """
                    select count(s) from AttendanceSessionEntity s join EvaluationEntity e on e.id = s.evaluationId
                    where e.teachingPeriodId = :teachingPeriodId""")
    Page<AttendanceSessionView> findViews(@Param("teachingPeriodId") Long teachingPeriodId, Pageable pageable);
}
