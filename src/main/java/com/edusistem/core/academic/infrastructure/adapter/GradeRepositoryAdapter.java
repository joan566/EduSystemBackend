package com.edusistem.core.academic.infrastructure.adapter;

import com.edusistem.core.academic.domain.entity.Grade;
import com.edusistem.core.academic.domain.outputports.GradeRepositoryPort;
import com.edusistem.core.academic.infrastructure.mapper.AcademicMapper;
import com.edusistem.core.academic.infrastructure.repository.SpringDataGradeRepository;
import com.edusistem.core.shared.infrastructure.adapter.SqlSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GradeRepositoryAdapter implements GradeRepositoryPort {

    private final SpringDataGradeRepository repository;
    private final AcademicMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public GradeRepositoryAdapter(SpringDataGradeRepository repository, AcademicMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Grade save(Grade grade) {
        return mapper.toDomain(repository.save(mapper.toEntity(grade)));
    }

    @Override
    public Optional<Grade> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Grade> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<Grade> findAllOrderedByName() {
        return repository.findAllByOrderByNameAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean hasGroups(Long gradeId) {
        return SqlSupport.exists(em, "select 1 from groups where grade_id = :id", "id", gradeId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
