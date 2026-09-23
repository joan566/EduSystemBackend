package com.edusistem.core.exam.infrastructure.entity;

import com.edusistem.core.shared.infrastructure.entity.CreatedAtEntity;
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
@Table(name = "exam_questions")
public class ExamQuestionEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "question_number", nullable = false)
    private int questionNumber;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal points;
}
