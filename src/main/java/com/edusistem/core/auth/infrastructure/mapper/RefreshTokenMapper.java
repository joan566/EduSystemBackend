package com.edusistem.core.auth.infrastructure.mapper;

import com.edusistem.core.auth.domain.entity.RefreshToken;
import com.edusistem.core.auth.infrastructure.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

    RefreshToken toDomain(RefreshTokenEntity entity);

    RefreshTokenEntity toEntity(RefreshToken token);
}
