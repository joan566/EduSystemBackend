package com.edusistem.core.subject.application.use_case.dtos;

public final class SubjectCommands {

    private SubjectCommands() {
    }

    public record Create(Long teacherId, String name, String description) {
    }

    public record Update(Long teacherId, Long subjectId, String name, String description) {
    }
}
