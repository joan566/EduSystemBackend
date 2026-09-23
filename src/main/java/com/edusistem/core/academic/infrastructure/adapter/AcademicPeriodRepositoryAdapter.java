package com.edusistem.core.academic.infrastructure.adapter;

import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.outputports.AcademicPeriodRepositoryPort;
import com.edusistem.core.academic.infrastructure.mapper.AcademicMapper;
import com.edusistem.core.academic.infrastructure.repository.SpringDataAcademicPeriodRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import com.edusistem.core.shared.infrastructure.adapter.SqlSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AcademicPeriodRepositoryAdapter implements AcademicPeriodRepositoryPort {

    private final SpringDataAcademicPeriodRepository repository;
    private final AcademicMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public AcademicPeriodRepositoryAdapter(SpringDataAcademicPeriodRepository repository, AcademicMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AcademicPeriod save(AcademicPeriod period) {
        return mapper.toDomain(repository.save(mapper.toEntity(period)));
    }

    @Override
    public Optional<AcademicPeriod> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AcademicPeriod> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public PageResult<AcademicPeriod> findAll(PageQuery page) {
        return PageMapper.toResult(repository.findAll(PageMapper.pageable(page, Sort.by(Sort.Direction.DESC, "startDate"))),
                mapper::toDomain);
    }

    @Override
    public boolean hasTeachingPeriods(Long academicPeriodId) {
        return SqlSupport.exists(em, "select 1 from teaching_periods where academic_period_id = :id", "id", academicPeriodId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
