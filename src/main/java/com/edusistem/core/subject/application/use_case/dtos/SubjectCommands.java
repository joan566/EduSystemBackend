package com.edusistem.core.subject.application.use_case.dtos;

public final class SubjectCommands {

    private SubjectCommands() {
    }

    public record Create(Long actorId, String name, String description) {
    }

    public record Update(Long actorId, Long subjectId, String name, String description) {
    }
}
