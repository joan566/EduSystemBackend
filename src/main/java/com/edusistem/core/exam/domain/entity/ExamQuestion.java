package com.edusistem.core.exam.domain.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
public class ExamQuestion {
    private Long id;
    private Long examId;
    private int questionNumber;
    private String statement;
    private String correctOption;
    private BigDecimal points;
    @Builder.Default
    private List<ExamQuestionOption> options = new ArrayList<>();

    public boolean hasOption(String letter) {
        return options.stream().anyMatch(o -> o.getOptionLetter().equals(letter));
    }
}
