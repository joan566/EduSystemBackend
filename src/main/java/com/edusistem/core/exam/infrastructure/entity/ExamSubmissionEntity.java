package com.edusistem.core.exam.infrastructure.entity;

import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import com.edusistem.core.shared.infrastructure.entity.AuditableEntity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "exam_submissions")
public class ExamSubmissionEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_code", nullable = false, length = 50)
    private String studentCode;

    @Column(name = "detected_qr_data", columnDefinition = "text")
    private String detectedQrData;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExamSubmissionStatus status;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "final_grade", precision = 6, scale = 2)
    private BigDecimal finalGrade;

    @Column(name = "status_detail", length = 500)
    private String statusDetail;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
