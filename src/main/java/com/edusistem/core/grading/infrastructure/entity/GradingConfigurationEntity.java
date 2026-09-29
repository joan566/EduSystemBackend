package com.edusistem.core.grading.infrastructure.entity;

import com.edusistem.core.shared.infrastructure.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "grading_configurations")
public class GradingConfigurationEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teaching_period_id", nullable = false)
    private Long teachingPeriodId;

    @Column(name = "grading_scale_id", nullable = false)
    private Long gradingScaleId;

    @Column(name = "passing_grade", precision = 6, scale = 2)
    private BigDecimal passingGrade;
}
