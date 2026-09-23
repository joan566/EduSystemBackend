package com.edusistem.core.user.application.use_case.dtos;

public record UpdateProfileCommand(Long userId, String firstName, String lastName) {
}
