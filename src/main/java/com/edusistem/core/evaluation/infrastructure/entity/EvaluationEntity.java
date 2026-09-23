package com.edusistem.core.evaluation.infrastructure.entity;

import com.edusistem.core.shared.infrastructure.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "evaluations")
public class EvaluationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teaching_period_id", nullable = false)
    private Long teachingPeriodId;

    @Column(name = "evaluation_category_id", nullable = false)
    private Long evaluationCategoryId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "evaluation_date")
    private LocalDateTime evaluationDate;

    @Column(name = "maximum_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal maximumScore;
}
