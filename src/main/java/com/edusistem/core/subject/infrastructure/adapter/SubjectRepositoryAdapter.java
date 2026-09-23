package com.edusistem.core.subject.infrastructure.adapter;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import com.edusistem.core.shared.infrastructure.adapter.SqlSupport;
import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import com.edusistem.core.subject.infrastructure.mapper.SubjectMapper;
import com.edusistem.core.subject.infrastructure.repository.SpringDataSubjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class SubjectRepositoryAdapter implements SubjectRepositoryPort {

    private final SpringDataSubjectRepository repository;
    private final SubjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public SubjectRepositoryAdapter(SpringDataSubjectRepository repository, SubjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Subject save(Subject subject) {
        return mapper.toDomain(repository.save(mapper.toEntity(subject)));
    }

    @Override
    public Optional<Subject> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Subject> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public PageResult<Subject> search(String nameQuery, PageQuery page) {
        String pattern = "%" + (nameQuery == null ? "" : nameQuery.trim().toLowerCase(Locale.ROOT)) + "%";
        return PageMapper.toResult(repository.search(pattern, PageMapper.pageable(page, Sort.by("name"))), mapper::toDomain);
    }

    @Override
    public boolean hasTeachingAssignments(Long subjectId) {
        return SqlSupport.exists(em, "select 1 from teaching_assignments where subject_id = :id", "id", subjectId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
