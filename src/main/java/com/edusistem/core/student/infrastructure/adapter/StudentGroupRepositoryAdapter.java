package com.edusistem.core.student.infrastructure.adapter;

import com.edusistem.core.student.domain.entity.StudentGroup;
import com.edusistem.core.student.domain.outputports.StudentGroupRepositoryPort;
import com.edusistem.core.student.domain.vo.StudentEnrollmentView;
import com.edusistem.core.student.infrastructure.entity.StudentGroupId;
import com.edusistem.core.student.infrastructure.mapper.StudentMapper;
import com.edusistem.core.student.infrastructure.repository.SpringDataStudentGroupRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StudentGroupRepositoryAdapter implements StudentGroupRepositoryPort {

    private final SpringDataStudentGroupRepository repository;
    private final StudentMapper mapper;

    public StudentGroupRepositoryAdapter(SpringDataStudentGroupRepository repository, StudentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public StudentGroup save(StudentGroup studentGroup) {
        return mapper.toDomain(repository.save(mapper.toEntity(studentGroup)));
    }

    @Override
    public Optional<StudentGroup> find(Long studentId, Long groupId) {
        StudentGroupId id = new StudentGroupId();
        id.setStudentId(studentId);
        id.setGroupId(groupId);
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsActive(Long studentId, Long groupId) {
        return repository.existsByStudentIdAndGroupIdAndActiveTrue(studentId, groupId);
    }

    @Override
    public List<StudentEnrollmentView> findEnrollmentViews(Long studentId) {
        return repository.findEnrollmentViews(studentId);
    }
}
