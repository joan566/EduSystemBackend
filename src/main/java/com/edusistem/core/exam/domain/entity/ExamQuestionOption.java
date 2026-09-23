package com.edusistem.core.exam.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionOption {
    private Long id;
    private Long questionId;
    private String optionLetter;
    private String optionText;
}
