package com.edusistem.core.user.infrastructure.mapper;

import com.edusistem.core.user.domain.entity.User;
import com.edusistem.core.user.infrastructure.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /** Los roles viven en user_roles y los completa el adapter. */
    @Mapping(target = "roles", ignore = true)
    User toDomain(UserEntity entity);

    UserEntity toEntity(User user);
}
