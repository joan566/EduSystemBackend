package com.edusistem.core.subject.infrastructure.mapper;

import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.infrastructure.entity.SubjectEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubjectMapper {

    Subject toDomain(SubjectEntity entity);

    SubjectEntity toEntity(Subject subject);
}
