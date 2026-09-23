package com.edusistem.core.student.infrastructure.adapter;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.outputports.StudentRepositoryPort;
import com.edusistem.core.student.infrastructure.mapper.StudentMapper;
import com.edusistem.core.student.infrastructure.repository.SpringDataStudentRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StudentRepositoryAdapter implements StudentRepositoryPort {

    private final SpringDataStudentRepository repository;
    private final StudentMapper mapper;

    public StudentRepositoryAdapter(SpringDataStudentRepository repository, StudentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Student save(Student student) {
        return mapper.toDomain(repository.save(mapper.toEntity(student)));
    }

    @Override
    public Optional<Student> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Student> findByStudentCode(String studentCode) {
        return repository.findByStudentCode(studentCode).map(mapper::toDomain);
    }

    @Override
    public Optional<Student> findByIdentificationNumber(String identificationNumber) {
        return repository.findByIdentificationNumber(identificationNumber).map(mapper::toDomain);
    }

    @Override
    public PageResult<Student> searchByTeacher(Long teacherId, Long groupId, String search, PageQuery page) {
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase(Locale.ROOT)) + "%";
        return PageMapper.toResult(repository.searchByTeacher(teacherId, groupId, pattern, PageMapper.pageable(page)),
                mapper::toDomain);
    }

    @Override
    public List<Student> findActiveByGroupId(Long groupId) {
        return repository.findActiveByGroupId(groupId).stream().map(mapper::toDomain).toList();
    }
}
