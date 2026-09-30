package com.edusistem.core.subject.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.subject.application.use_case.dtos.SubjectCommands;
import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.domain.inputports.ManageSubjectUseCase;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;

/** Asignaturas propias del profesor; las de otros profesores responden 404. */
public class SubjectService implements ManageSubjectUseCase {

    private final SubjectRepositoryPort subjects;
    private final RecordAuditUseCase audit;

    public SubjectService(SubjectRepositoryPort subjects, RecordAuditUseCase audit) {
        this.subjects = subjects;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public Subject create(SubjectCommands.Create command) {
        String name = command.name().trim();
        requireNameAvailable(command.teacherId(), name, null);
        Subject saved = subjects.save(Subject.builder().teacherId(command.teacherId()).name(name)
                .description(blankToNull(command.description())).build());
        audit.success(command.teacherId(), AuditAction.CREATE, "Subject", saved.getId(), name);
        return saved;
    }

    @Override
    @UseCaseTransactional
    public Subject update(SubjectCommands.Update command) {
        Subject subject = get(command.teacherId(), command.subjectId());
        String name = command.name().trim();
        requireNameAvailable(command.teacherId(), name, subject.getId());
        subject.setName(name);
        subject.setDescription(blankToNull(command.description()));
        Subject saved = subjects.save(subject);
        audit.success(command.teacherId(), AuditAction.UPDATE, "Subject", saved.getId(), name);
        return saved;
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long subjectId) {
        Subject subject = get(teacherId, subjectId);
        if (subjects.hasTeachingAssignments(subjectId)) {
            throw new ConflictException("SUBJECT_HAS_TEACHING_ASSIGNMENTS",
                    "The subject has teaching assignments and cannot be deleted");
        }
        subjects.deleteById(subjectId);
        audit.success(teacherId, AuditAction.DELETE, "Subject", subjectId, subject.getName());
    }

    @Override
    public Subject get(Long teacherId, Long subjectId) {
        return subjects.findById(subjectId).filter(s -> s.getTeacherId().equals(teacherId))
                .orElseThrow(() -> ResourceNotFoundException.of("Subject", subjectId));
    }

    @Override
    public PageResult<Subject> search(Long teacherId, String nameQuery, PageQuery page) {
        return subjects.search(teacherId, nameQuery, page);
    }

    private void requireNameAvailable(Long teacherId, String name, Long currentId) {
        subjects.findByTeacherIdAndName(teacherId, name).filter(s -> !s.getId().equals(currentId)).ifPresent(s -> {
            throw new ConflictException("SUBJECT_ALREADY_EXISTS", "A subject named '" + name + "' already exists");
        });
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
