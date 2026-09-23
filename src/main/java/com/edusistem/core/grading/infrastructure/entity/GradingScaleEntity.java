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
@Table(name = "grading_scales")
public class GradingScaleEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "minimum_value", nullable = false, precision = 6, scale = 2)
    private BigDecimal minimumValue;

    @Column(name = "maximum_value", nullable = false, precision = 6, scale = 2)
    private BigDecimal maximumValue;
}
