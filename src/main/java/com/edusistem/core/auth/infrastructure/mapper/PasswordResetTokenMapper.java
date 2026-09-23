package com.edusistem.core.auth.infrastructure.mapper;

import com.edusistem.core.auth.domain.entity.PasswordResetToken;
import com.edusistem.core.auth.infrastructure.entity.PasswordResetTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PasswordResetTokenMapper {

    PasswordResetToken toDomain(PasswordResetTokenEntity entity);

    PasswordResetTokenEntity toEntity(PasswordResetToken token);
}
