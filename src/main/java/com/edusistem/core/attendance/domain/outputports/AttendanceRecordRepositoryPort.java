package com.edusistem.core.attendance.domain.outputports;

import com.edusistem.core.attendance.domain.entity.AttendanceRecord;
import java.util.List;

public interface AttendanceRecordRepositoryPort {

    List<AttendanceRecord> saveAll(List<AttendanceRecord> records);

    List<AttendanceRecord> findBySessionId(Long attendanceSessionId);

    boolean existsBySessionId(Long attendanceSessionId);
}
