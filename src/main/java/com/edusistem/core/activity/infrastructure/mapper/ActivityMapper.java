package com.edusistem.core.activity.infrastructure.mapper;

import com.edusistem.core.activity.domain.entity.Activity;
import com.edusistem.core.activity.domain.entity.ActivityGrade;
import com.edusistem.core.activity.infrastructure.entity.ActivityEntity;
import com.edusistem.core.activity.infrastructure.entity.ActivityGradeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActivityMapper {

    Activity toDomain(ActivityEntity entity);

    ActivityEntity toEntity(Activity activity);

    ActivityGrade toDomain(ActivityGradeEntity entity);

    ActivityGradeEntity toEntity(ActivityGrade grade);
}
