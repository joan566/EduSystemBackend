package com.edusistem.core.attendance.infrastructure.adapter;

import com.edusistem.core.attendance.domain.entity.AttendanceRecord;
import com.edusistem.core.attendance.domain.outputports.AttendanceRecordRepositoryPort;
import com.edusistem.core.attendance.infrastructure.mapper.AttendanceMapper;
import com.edusistem.core.attendance.infrastructure.repository.SpringDataAttendanceRecordRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AttendanceRecordRepositoryAdapter implements AttendanceRecordRepositoryPort {

    private final SpringDataAttendanceRecordRepository repository;
    private final AttendanceMapper mapper;

    public AttendanceRecordRepositoryAdapter(SpringDataAttendanceRecordRepository repository, AttendanceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<AttendanceRecord> saveAll(List<AttendanceRecord> records) {
        return repository.saveAll(records.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AttendanceRecord> findBySessionId(Long attendanceSessionId) {
        return repository.findByAttendanceSessionId(attendanceSessionId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsBySessionId(Long attendanceSessionId) {
        return repository.existsByAttendanceSessionId(attendanceSessionId);
    }
}
