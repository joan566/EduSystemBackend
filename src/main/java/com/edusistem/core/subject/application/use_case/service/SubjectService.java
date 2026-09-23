package com.edusistem.core.subject.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.CatalogUsagePort;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.subject.application.use_case.dtos.SubjectCommands;
import com.edusistem.core.subject.domain.entity.Subject;
import com.edusistem.core.subject.domain.inputports.ManageSubjectUseCase;
import com.edusistem.core.subject.domain.outputports.SubjectRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService implements ManageSubjectUseCase {

    private final SubjectRepositoryPort subjects;
    private final CatalogUsagePort usage;
    private final RecordAuditUseCase audit;

    public SubjectService(SubjectRepositoryPort subjects, CatalogUsagePort usage, RecordAuditUseCase audit) {
        this.subjects = subjects;
        this.usage = usage;
        this.audit = audit;
    }

    @Override
    @Transactional
    public Subject create(SubjectCommands.Create command) {
        String name = command.name().trim();
        requireNameAvailable(name, null);
        Subject saved = subjects.save(Subject.builder().name(name).description(blankToNull(command.description())).build());
        audit.success(command.actorId(), AuditAction.CREATE, "Subject", saved.getId(), name);
        return saved;
    }

    @Override
    @Transactional
    public Subject update(SubjectCommands.Update command) {
        Subject subject = get(command.subjectId());
        if (usage.subjectUsedByOtherTeachers(subject.getId(), command.actorId())) {
            throw new ConflictException("CATALOG_ITEM_IN_USE",
                    "The subject is assigned to other teachers and cannot be modified");
        }
        String name = command.name().trim();
        requireNameAvailable(name, subject.getId());
        subject.setName(name);
        subject.setDescription(blankToNull(command.description()));
        Subject saved = subjects.save(subject);
        audit.success(command.actorId(), AuditAction.UPDATE, "Subject", saved.getId(), name);
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long actorId, Long subjectId) {
        Subject subject = get(subjectId);
        if (subjects.hasTeachingAssignments(subjectId)) {
            throw new ConflictException("SUBJECT_HAS_TEACHING_ASSIGNMENTS",
                    "The subject has teaching assignments and cannot be deleted");
        }
        subjects.deleteById(subjectId);
        audit.success(actorId, AuditAction.DELETE, "Subject", subjectId, subject.getName());
    }

    @Override
    public Subject get(Long subjectId) {
        return subjects.findById(subjectId).orElseThrow(() -> ResourceNotFoundException.of("Subject", subjectId));
    }

    @Override
    public PageResult<Subject> search(String nameQuery, PageQuery page) {
        return subjects.search(nameQuery, page);
    }

    private void requireNameAvailable(String name, Long currentId) {
        subjects.findByName(name).filter(s -> !s.getId().equals(currentId)).ifPresent(s -> {
            throw new ConflictException("SUBJECT_ALREADY_EXISTS", "A subject named '" + name + "' already exists");
        });
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
