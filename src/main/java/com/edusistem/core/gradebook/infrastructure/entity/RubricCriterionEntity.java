package com.edusistem.core.gradebook.infrastructure.entity;

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
@Table(name = "rubric_criteria")
public class RubricCriterionEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;
}
