package com.edusistem.core.attendance.infrastructure.mapper;

import com.edusistem.core.attendance.domain.entity.AttendanceRecord;
import com.edusistem.core.attendance.domain.entity.AttendanceSession;
import com.edusistem.core.attendance.infrastructure.entity.AttendanceRecordEntity;
import com.edusistem.core.attendance.infrastructure.entity.AttendanceSessionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    AttendanceSession toDomain(AttendanceSessionEntity entity);

    AttendanceSessionEntity toEntity(AttendanceSession session);

    AttendanceRecord toDomain(AttendanceRecordEntity entity);

    AttendanceRecordEntity toEntity(AttendanceRecord record);
}
