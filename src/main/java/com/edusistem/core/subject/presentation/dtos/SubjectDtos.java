package com.edusistem.core.subject.presentation.dtos;

import com.edusistem.core.subject.domain.entity.Subject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SubjectDtos {

    private SubjectDtos() {
    }

    public record Request(@NotBlank @Size(max = 100) String name, @Size(max = 255) String description) {
    }

    public record Response(Long id, String name, String description) {

        public static Response from(Subject subject) {
            return new Response(subject.getId(), subject.getName(), subject.getDescription());
        }
    }
}
