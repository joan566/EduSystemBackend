package com.edusistem.core.exam.infrastructure.entity;

import com.edusistem.core.shared.infrastructure.entity.CreatedAtEntity;
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
@Table(name = "exam_question_options")
public class ExamQuestionOptionEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "option_letter", nullable = false, length = 1)
    private String optionLetter;

    @Column(name = "option_text", nullable = false, columnDefinition = "text")
    private String optionText;
}
