package com.edusistem.core.grading.infrastructure.entity;

import com.edusistem.core.shared.infrastructure.entity.CreatedAtEntity;
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
@Table(name = "grading_weights")
public class GradingWeightEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grading_configuration_id", nullable = false)
    private Long gradingConfigurationId;

    @Column(name = "evaluation_category_id", nullable = false)
    private Long evaluationCategoryId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;
}
