package com.edusistem.core.academic.infrastructure.adapter;

import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.outputports.GroupRepositoryPort;
import com.edusistem.core.academic.domain.vo.GroupView;
import com.edusistem.core.academic.infrastructure.mapper.AcademicMapper;
import com.edusistem.core.academic.infrastructure.repository.SpringDataGroupRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import com.edusistem.core.shared.infrastructure.adapter.SqlSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GroupRepositoryAdapter implements GroupRepositoryPort {

    private final SpringDataGroupRepository repository;
    private final AcademicMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public GroupRepositoryAdapter(SpringDataGroupRepository repository, AcademicMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Group save(Group group) {
        return mapper.toDomain(repository.save(mapper.toEntity(group)));
    }

    @Override
    public Optional<Group> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<GroupView> findViewById(Long id) {
        return repository.findViewById(id);
    }

    @Override
    public Optional<Group> findByGradeIdAndNameAndAcademicYear(Long gradeId, String name, int academicYear) {
        return repository.findByGradeIdAndNameAndAcademicYear(gradeId, name, academicYear).map(mapper::toDomain);
    }

    @Override
    public Optional<Group> findByGradeNameAndNameAndAcademicYear(String gradeName, String name, int academicYear) {
        return repository.findByGradeNameAndName(gradeName, name, academicYear).map(mapper::toDomain);
    }

    @Override
    public PageResult<GroupView> search(Long gradeId, Integer academicYear, PageQuery page) {
        return PageMapper.toResult(repository.searchViews(gradeId, academicYear, PageMapper.pageable(page)), v -> v);
    }

    @Override
    public boolean hasDependents(Long groupId) {
        return SqlSupport.exists(em, "select 1 from student_groups where group_id = :id", "id", groupId)
                || SqlSupport.exists(em, "select 1 from teaching_assignments where group_id = :id", "id", groupId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
