package com.edusistem.core.gradebook.infrastructure.adapter;

import com.edusistem.core.gradebook.domain.entity.GradeAttachment;
import com.edusistem.core.gradebook.domain.outputports.GradeAttachmentRepositoryPort;
import com.edusistem.core.gradebook.infrastructure.entity.GradeAttachmentEntity;
import com.edusistem.core.gradebook.infrastructure.repository.SpringDataGradeAttachmentRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GradeAttachmentRepositoryAdapter implements GradeAttachmentRepositoryPort {

    private final SpringDataGradeAttachmentRepository repository;

    public GradeAttachmentRepositoryAdapter(SpringDataGradeAttachmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<GradeAttachment> find(Long evaluationId, Long studentId) {
        return repository.findByEvaluationIdAndStudentId(evaluationId, studentId).map(GradeAttachmentRepositoryAdapter::toDomain);
    }

    @Override
    public GradeAttachment save(GradeAttachment a) {
        GradeAttachmentEntity e = a.getId() == null ? new GradeAttachmentEntity()
                : repository.findById(a.getId()).orElseGet(GradeAttachmentEntity::new);
        e.setEvaluationId(a.getEvaluationId());
        e.setStudentId(a.getStudentId());
        e.setFileName(a.getFileName());
        e.setContentType(a.getContentType());
        e.setSizeBytes(a.getSizeBytes());
        e.setStoragePath(a.getStoragePath());
        return toDomain(repository.save(e));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Set<Long> evaluationIdsWithAttachment(Long teachingPeriodId, Long studentId) {
        return new HashSet<>(repository.findEvaluationIds(teachingPeriodId, studentId));
    }

    private static GradeAttachment toDomain(GradeAttachmentEntity e) {
        return GradeAttachment.builder().id(e.getId()).evaluationId(e.getEvaluationId()).studentId(e.getStudentId())
                .fileName(e.getFileName()).contentType(e.getContentType()).sizeBytes(e.getSizeBytes())
                .storagePath(e.getStoragePath()).updatedAt(e.getUpdatedAt()).build();
    }
}
