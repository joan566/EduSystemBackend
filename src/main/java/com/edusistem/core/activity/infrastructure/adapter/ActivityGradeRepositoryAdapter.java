package com.edusistem.core.activity.infrastructure.adapter;

import com.edusistem.core.activity.domain.entity.ActivityGrade;
import com.edusistem.core.activity.domain.outputports.ActivityGradeRepositoryPort;
import com.edusistem.core.activity.infrastructure.mapper.ActivityMapper;
import com.edusistem.core.activity.infrastructure.repository.SpringDataActivityGradeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ActivityGradeRepositoryAdapter implements ActivityGradeRepositoryPort {

    private final SpringDataActivityGradeRepository repository;
    private final ActivityMapper mapper;

    public ActivityGradeRepositoryAdapter(SpringDataActivityGradeRepository repository, ActivityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<ActivityGrade> saveAll(List<ActivityGrade> grades) {
        return repository.saveAll(grades.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ActivityGrade> findByActivityId(Long activityId) {
        return repository.findByActivityId(activityId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ActivityGrade> findByActivityIdAndStudentId(Long activityId, Long studentId) {
        return repository.findByActivityIdAndStudentId(activityId, studentId).map(mapper::toDomain);
    }

    @Override
    public Optional<BigDecimal> findMaximumGrade(Long activityId) {
        return Optional.ofNullable(repository.findMaximumGrade(activityId));
    }

    @Override
    public boolean existsByActivityId(Long activityId) {
        return repository.existsByActivityId(activityId);
    }
}
