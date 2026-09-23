package com.edusistem.core.academic.infrastructure.adapter;

import com.edusistem.core.academic.domain.entity.TeachingAssignment;
import com.edusistem.core.academic.domain.outputports.TeachingAssignmentRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingAssignmentView;
import com.edusistem.core.academic.infrastructure.mapper.AcademicMapper;
import com.edusistem.core.academic.infrastructure.repository.SpringDataTeachingAssignmentRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import com.edusistem.core.shared.infrastructure.adapter.SqlSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TeachingAssignmentRepositoryAdapter implements TeachingAssignmentRepositoryPort {

    private final SpringDataTeachingAssignmentRepository repository;
    private final AcademicMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public TeachingAssignmentRepositoryAdapter(SpringDataTeachingAssignmentRepository repository, AcademicMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TeachingAssignment save(TeachingAssignment assignment) {
        return mapper.toDomain(repository.save(mapper.toEntity(assignment)));
    }

    @Override
    public Optional<TeachingAssignment> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TeachingAssignmentView> findViewById(Long id) {
        return repository.findViewById(id);
    }

    @Override
    public Optional<TeachingAssignment> findByTeacherIdAndGroupIdAndSubjectId(Long teacherId, Long groupId, Long subjectId) {
        return repository.findByTeacherIdAndGroupIdAndSubjectId(teacherId, groupId, subjectId).map(mapper::toDomain);
    }

    @Override
    public PageResult<TeachingAssignmentView> findViewsByTeacherId(Long teacherId, Long groupId, Long subjectId,
                                                                   Boolean active, PageQuery page) {
        return PageMapper.toResult(repository.findViewsByTeacher(teacherId, groupId, subjectId, active,
                PageMapper.pageable(page)), v -> v);
    }

    @Override
    public boolean hasTeachingPeriods(Long teachingAssignmentId) {
        return SqlSupport.exists(em, "select 1 from teaching_periods where teaching_assignment_id = :id",
                "id", teachingAssignmentId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
