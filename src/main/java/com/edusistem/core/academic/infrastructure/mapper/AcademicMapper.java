package com.edusistem.core.academic.infrastructure.mapper;

import com.edusistem.core.academic.domain.entity.AcademicPeriod;
import com.edusistem.core.academic.domain.entity.Grade;
import com.edusistem.core.academic.domain.entity.Group;
import com.edusistem.core.academic.domain.entity.TeachingAssignment;
import com.edusistem.core.academic.domain.entity.TeachingPeriod;
import com.edusistem.core.academic.infrastructure.entity.AcademicPeriodEntity;
import com.edusistem.core.academic.infrastructure.entity.GradeEntity;
import com.edusistem.core.academic.infrastructure.entity.GroupEntity;
import com.edusistem.core.academic.infrastructure.entity.TeachingAssignmentEntity;
import com.edusistem.core.academic.infrastructure.entity.TeachingPeriodEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AcademicMapper {

    Grade toDomain(GradeEntity entity);

    GradeEntity toEntity(Grade grade);

    Group toDomain(GroupEntity entity);

    GroupEntity toEntity(Group group);

    AcademicPeriod toDomain(AcademicPeriodEntity entity);

    AcademicPeriodEntity toEntity(AcademicPeriod period);

    TeachingAssignment toDomain(TeachingAssignmentEntity entity);

    TeachingAssignmentEntity toEntity(TeachingAssignment assignment);

    TeachingPeriod toDomain(TeachingPeriodEntity entity);

    TeachingPeriodEntity toEntity(TeachingPeriod teachingPeriod);
}
