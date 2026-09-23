package com.edusistem.core.activity.infrastructure.adapter;

import com.edusistem.core.activity.domain.entity.Activity;
import com.edusistem.core.activity.domain.outputports.ActivityRepositoryPort;
import com.edusistem.core.activity.domain.vo.ActivityView;
import com.edusistem.core.activity.infrastructure.mapper.ActivityMapper;
import com.edusistem.core.activity.infrastructure.repository.SpringDataActivityRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ActivityRepositoryAdapter implements ActivityRepositoryPort {

    private final SpringDataActivityRepository repository;
    private final ActivityMapper mapper;

    public ActivityRepositoryAdapter(SpringDataActivityRepository repository, ActivityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Activity save(Activity activity) {
        return mapper.toDomain(repository.save(mapper.toEntity(activity)));
    }

    @Override
    public Optional<Activity> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ActivityView> findViewById(Long id) {
        return repository.findViewById(id);
    }

    @Override
    public PageResult<ActivityView> findViewsByTeachingPeriodId(Long teachingPeriodId, PageQuery page) {
        return PageMapper.toResult(repository.findViews(teachingPeriodId, PageMapper.pageable(page)), v -> v);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
