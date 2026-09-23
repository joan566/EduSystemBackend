package com.edusistem.core.authorization.infrastructure.mapper;

import com.edusistem.core.authorization.domain.entity.Role;
import com.edusistem.core.authorization.infrastructure.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    Role toDomain(RoleEntity entity);
}
