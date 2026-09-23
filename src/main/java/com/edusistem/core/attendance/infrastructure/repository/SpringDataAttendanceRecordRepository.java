package com.edusistem.core.attendance.infrastructure.repository;

import com.edusistem.core.attendance.infrastructure.entity.AttendanceRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, Long> {

    List<AttendanceRecordEntity> findByAttendanceSessionId(Long attendanceSessionId);

    boolean existsByAttendanceSessionId(Long attendanceSessionId);
}
