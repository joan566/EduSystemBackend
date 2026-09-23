package com.edusistem.core.student.infrastructure.mapper;

import com.edusistem.core.student.domain.entity.Student;
import com.edusistem.core.student.domain.entity.StudentGroup;
import com.edusistem.core.student.infrastructure.entity.StudentEntity;
import com.edusistem.core.student.infrastructure.entity.StudentGroupEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    Student toDomain(StudentEntity entity);

    StudentEntity toEntity(Student student);

    StudentGroup toDomain(StudentGroupEntity entity);

    StudentGroupEntity toEntity(StudentGroup studentGroup);
}
