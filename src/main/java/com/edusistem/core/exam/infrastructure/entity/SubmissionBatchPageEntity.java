package com.edusistem.core.exam.infrastructure.entity;

import com.edusistem.core.exam.domain.enums.BatchPageOutcome;
import com.edusistem.core.shared.infrastructure.entity.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_submission_batch_pages")
public class SubmissionBatchPageEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchPageOutcome outcome;

    @Column(name = "error_code", length = 60)
    private String errorCode;

    @Column(length = 500)
    private String message;

    @Column(name = "student_code", length = 50)
    private String studentCode;

    @Column(name = "submission_id")
    private Long submissionId;
}
