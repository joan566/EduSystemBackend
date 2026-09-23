package com.edusistem.core.attendance.infrastructure.adapter;

import com.edusistem.core.attendance.domain.entity.AttendanceSession;
import com.edusistem.core.attendance.domain.outputports.AttendanceSessionRepositoryPort;
import com.edusistem.core.attendance.domain.vo.AttendanceSessionView;
import com.edusistem.core.attendance.infrastructure.mapper.AttendanceMapper;
import com.edusistem.core.attendance.infrastructure.repository.SpringDataAttendanceSessionRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AttendanceSessionRepositoryAdapter implements AttendanceSessionRepositoryPort {

    private final SpringDataAttendanceSessionRepository repository;
    private final AttendanceMapper mapper;

    public AttendanceSessionRepositoryAdapter(SpringDataAttendanceSessionRepository repository, AttendanceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AttendanceSession save(AttendanceSession session) {
        return mapper.toDomain(repository.save(mapper.toEntity(session)));
    }

    @Override
    public Optional<AttendanceSession> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AttendanceSessionView> findViewById(Long id) {
        return repository.findViewById(id);
    }

    @Override
    public PageResult<AttendanceSessionView> findViewsByTeachingPeriodId(Long teachingPeriodId, PageQuery page) {
        return PageMapper.toResult(repository.findViews(teachingPeriodId, PageMapper.pageable(page)), v -> v);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
