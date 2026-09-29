package com.edusistem.core.gradebook.application.use_case.service;

import com.edusistem.core.audit.domain.enums.AuditAction;
import com.edusistem.core.audit.domain.inputports.RecordAuditUseCase;
import com.edusistem.core.audit.domain.vo.AuditTarget;
import com.edusistem.core.gradebook.application.use_case.dtos.GradebookCommands;
import com.edusistem.core.gradebook.domain.entity.GradeAttachment;
import com.edusistem.core.gradebook.domain.inputports.ManageGradeAttachmentUseCase;
import com.edusistem.core.gradebook.domain.outputports.GradeAttachmentRepositoryPort;
import com.edusistem.core.gradebook.domain.outputports.GradebookQueryPort;
import com.edusistem.core.gradebook.domain.outputports.GradebookQueryPort.EvaluationKind;
import com.edusistem.core.gradebook.domain.vo.EvaluationType;
import com.edusistem.core.gradebook.domain.vo.StoredFile;
import com.edusistem.core.shared.application.service.OwnershipGuard;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;
import com.edusistem.core.shared.domain.exceptions.ConflictException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import com.edusistem.core.shared.domain.exceptions.ResourceNotFoundException;
import com.edusistem.core.shared.domain.outputports.FileStoragePort;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/** Un archivo por nota (p. ej. el trabajo entregado); subir otro lo reemplaza. No aplica a la asistencia. */
public class GradeAttachmentService implements ManageGradeAttachmentUseCase {

    static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    /** Extensiones permitidas y el tipo con el que se sirven (no se confía en el que envía el cliente). */
    static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("txt", "text/plain"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"));

    private final GradebookQueryPort query;
    private final GradeAttachmentRepositoryPort attachments;
    private final GradebookService gradebook;
    private final FileStoragePort storage;
    private final OwnershipGuard guard;
    private final RecordAuditUseCase audit;

    public GradeAttachmentService(GradebookQueryPort query, GradeAttachmentRepositoryPort attachments,
                                  GradebookService gradebook, FileStoragePort storage, OwnershipGuard guard,
                                  RecordAuditUseCase audit) {
        this.query = query;
        this.attachments = attachments;
        this.gradebook = gradebook;
        this.storage = storage;
        this.guard = guard;
        this.audit = audit;
    }

    @Override
    @UseCaseTransactional
    public GradeAttachment upload(GradebookCommands.UploadAttachment command) {
        guard.requireEvaluation(command.teacherId(), command.evaluationId());
        guard.requireStudent(command.teacherId(), command.studentId());
        EvaluationKind kind = query.findKind(command.evaluationId())
                .orElseThrow(() -> ResourceNotFoundException.of("Evaluation", command.evaluationId()));
        if (kind.type() == EvaluationType.ATTENDANCE) {
            throw new ConflictException("ATTACHMENT_NOT_SUPPORTED", "Attendance grades cannot have attachments");
        }
        var student = gradebook.enrolledStudent(kind.teachingPeriodId(), command.studentId());
        if (command.content() == null || command.content().length == 0) {
            throw new InvalidRequestException("EMPTY_FILE", "The file is empty");
        }
        if (command.content().length > MAX_SIZE_BYTES) {
            throw new InvalidRequestException("ATTACHMENT_TOO_LARGE", "The file cannot exceed 10 MB");
        }
        String fileName = baseName(command.fileName());
        String contentType = CONTENT_TYPES.get(extension(fileName));
        if (contentType == null) {
            throw new InvalidRequestException("ATTACHMENT_TYPE_NOT_ALLOWED",
                    "Allowed files: " + String.join(", ", CONTENT_TYPES.keySet().stream().sorted().toList()));
        }

        GradeAttachment existing = attachments.find(command.evaluationId(), command.studentId()).orElse(null);
        String path = storage.store("grade-attachments/evaluation-" + command.evaluationId(), fileName, command.content());
        String previousPath = existing == null ? null : existing.getStoragePath();
        GradeAttachment attachment = existing != null ? existing : GradeAttachment.builder()
                .evaluationId(command.evaluationId()).studentId(command.studentId()).build();
        attachment.setFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setSizeBytes(command.content().length);
        attachment.setStoragePath(path);
        GradeAttachment saved = attachments.save(attachment);
        deleteQuietly(previousPath);
        audit.success(command.teacherId(), existing == null ? AuditAction.CREATE : AuditAction.UPDATE,
                AuditTarget.inTeachingPeriod("GradeAttachment", saved.getId(), kind.teachingPeriodId(),
                        AuditTarget.label(fileName, student.fullName())), fileName);
        return saved;
    }

    @Override
    public StoredFile download(Long teacherId, Long evaluationId, Long studentId) {
        GradeAttachment attachment = owned(teacherId, evaluationId, studentId);
        try {
            return new StoredFile(attachment.getFileName(), attachment.getContentType(),
                    storage.read(attachment.getStoragePath()));
        } catch (IOException e) {
            throw new ResourceNotFoundException("ATTACHMENT_FILE_MISSING", "The attachment file is no longer available");
        }
    }

    @Override
    @UseCaseTransactional
    public void delete(Long teacherId, Long evaluationId, Long studentId) {
        GradeAttachment attachment = owned(teacherId, evaluationId, studentId);
        attachments.delete(attachment.getId());
        deleteQuietly(attachment.getStoragePath());
        Long teachingPeriodId = query.findKind(evaluationId).map(EvaluationKind::teachingPeriodId).orElse(null);
        audit.success(teacherId, AuditAction.DELETE, AuditTarget.inTeachingPeriod("GradeAttachment",
                attachment.getId(), teachingPeriodId, attachment.getFileName()), attachment.getFileName());
    }

    private GradeAttachment owned(Long teacherId, Long evaluationId, Long studentId) {
        guard.requireEvaluation(teacherId, evaluationId);
        guard.requireStudent(teacherId, studentId);
        return attachments.find(evaluationId, studentId).orElseThrow(() -> new ResourceNotFoundException(
                "ATTACHMENT_NOT_FOUND", "This grade has no attachment"));
    }

    private void deleteQuietly(String path) {
        if (path == null) {
            return;
        }
        try {
            storage.delete(path);
        } catch (IOException ignored) {
            // Un archivo huérfano no debe impedir la operación.
        }
    }

    static String baseName(String fileName) {
        String name = fileName == null ? "" : fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.isEmpty() ? "archivo" : name;
    }

    static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
