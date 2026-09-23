package com.edusistem.core.exam.infrastructure.entity;

import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
import com.edusistem.core.shared.infrastructure.entity.CreatedAtEntity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
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
@Table(name = "exam_answers")
public class ExamAnswerEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "selected_option", length = 1)
    private String selectedOption;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "detection_confidence", precision = 5, scale = 4)
    private BigDecimal detectionConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_status", nullable = false, length = 20)
    private AnswerDetectionStatus detectionStatus;
}
