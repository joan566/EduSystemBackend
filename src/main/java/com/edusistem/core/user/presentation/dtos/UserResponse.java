package com.edusistem.core.user.presentation.dtos;

import com.edusistem.core.authorization.domain.enums.RoleName;
import com.edusistem.core.user.domain.entity.User;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(Long id, String firstName, String lastName, String email, boolean active,
                           List<String> roles, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.isActive(), user.getRoles().stream().map(RoleName::name).sorted().toList(), user.getCreatedAt());
    }
}
