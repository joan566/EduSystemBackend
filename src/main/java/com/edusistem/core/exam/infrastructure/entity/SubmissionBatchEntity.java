package com.edusistem.core.exam.infrastructure.entity;

import com.edusistem.core.exam.domain.enums.SubmissionBatchStatus;
import com.edusistem.core.shared.infrastructure.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_submission_batches")
public class SubmissionBatchEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_purged_at")
    private LocalDateTime filePurgedAt;

    @Column(name = "replace_existing", nullable = false)
    private boolean replaceExisting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionBatchStatus status;

    @Column(name = "status_detail", length = 500)
    private String statusDetail;

    @Column(name = "total_pages", nullable = false)
    private int totalPages;

    @Column(name = "processed_pages", nullable = false)
    private int processedPages;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
