package com.edusistem.core.academic.infrastructure.adapter;

import com.edusistem.core.academic.domain.entity.TeachingPeriodSchedule;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodScheduleRepositoryPort;
import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.academic.infrastructure.mapper.AcademicMapper;
import com.edusistem.core.academic.infrastructure.repository.SpringDataTeachingPeriodScheduleRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TeachingPeriodScheduleRepositoryAdapter implements TeachingPeriodScheduleRepositoryPort {

    private final SpringDataTeachingPeriodScheduleRepository repository;
    private final AcademicMapper mapper;

    public TeachingPeriodScheduleRepositoryAdapter(SpringDataTeachingPeriodScheduleRepository repository,
                                                   AcademicMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TeachingPeriodSchedule save(TeachingPeriodSchedule schedule) {
        return mapper.toDomain(repository.save(mapper.toEntity(schedule)));
    }

    @Override
    public Optional<TeachingPeriodSchedule> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ScheduledClassView> findViewById(Long id) {
        return repository.findViewById(id);
    }

    @Override
    public List<ScheduledClassView> findViewsByTeachingPeriodId(Long teachingPeriodId) {
        return repository.findViewsByTeachingPeriodId(teachingPeriodId);
    }

    @Override
    public Optional<ScheduledClassView> findConflict(Long teacherId, DayOfWeek dayOfWeek, LocalTime startTime,
                                                     LocalTime endTime, LocalDate periodStart, LocalDate periodEnd,
                                                     Long excludeScheduleId) {
        // los ids son positivos: 0 no excluye nada y evita el parámetro nulo
        return repository.findConflicts(teacherId, dayOfWeek, startTime, endTime, periodStart, periodEnd,
                excludeScheduleId == null ? 0L : excludeScheduleId).stream().findFirst();
    }

    @Override
    public List<ScheduledClassView> findViewsByTeacher(Long teacherId, LocalDate from, LocalDate to) {
        return repository.findViewsByTeacher(teacherId, from, to);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
