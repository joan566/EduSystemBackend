package com.edusistem.core.academic.infrastructure.adapter;

import com.edusistem.core.academic.domain.entity.TeachingPeriod;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.academic.infrastructure.mapper.AcademicMapper;
import com.edusistem.core.academic.infrastructure.repository.SpringDataTeachingPeriodRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import com.edusistem.core.shared.infrastructure.adapter.SqlSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TeachingPeriodRepositoryAdapter implements TeachingPeriodRepositoryPort {

    private final SpringDataTeachingPeriodRepository repository;
    private final AcademicMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public TeachingPeriodRepositoryAdapter(SpringDataTeachingPeriodRepository repository, AcademicMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TeachingPeriod save(TeachingPeriod teachingPeriod) {
        return mapper.toDomain(repository.save(mapper.toEntity(teachingPeriod)));
    }

    @Override
    public Optional<TeachingPeriod> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TeachingPeriodView> findViewById(Long id) {
        return repository.findViewById(id);
    }

    @Override
    public Optional<TeachingPeriod> findByTeachingAssignmentIdAndAcademicPeriodId(Long teachingAssignmentId,
                                                                                  Long academicPeriodId) {
        return repository.findByTeachingAssignmentIdAndAcademicPeriodId(teachingAssignmentId, academicPeriodId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<TeachingPeriodView> findViewsByTeacherId(Long teacherId, Long teachingAssignmentId,
                                                               Long academicPeriodId, PageQuery page) {
        return PageMapper.toResult(repository.findViewsByTeacher(teacherId, teachingAssignmentId, academicPeriodId,
                PageMapper.pageable(page)), v -> v);
    }

    @Override
    public boolean hasDependents(Long teachingPeriodId) {
        return SqlSupport.exists(em, "select 1 from evaluations where teaching_period_id = :id", "id", teachingPeriodId)
                || SqlSupport.exists(em, "select 1 from grading_configurations where teaching_period_id = :id",
                "id", teachingPeriodId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
