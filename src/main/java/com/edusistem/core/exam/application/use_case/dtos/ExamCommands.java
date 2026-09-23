package com.edusistem.core.exam.application.use_case.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ExamCommands {

    private ExamCommands() {
    }

    public record OptionInput(String letter, String text) {
    }

    /** {@code points} es opcional: por defecto maximumScore / número de preguntas. */
    public record QuestionInput(int questionNumber, String statement, String correctOption, BigDecimal points,
                                List<OptionInput> options) {
    }

    /** {@code maximumScore} es opcional (por defecto 1 punto por pregunta); {@code questions} es opcional. */
    public record Create(Long teacherId, Long teachingPeriodId, String name, String description,
                         LocalDateTime evaluationDate, BigDecimal maximumScore, int numberOfQuestions,
                         List<QuestionInput> questions) {
    }

    public record Update(Long teacherId, Long examId, String name, String description, LocalDateTime evaluationDate) {
    }

    public record ReplaceQuestions(Long teacherId, Long examId, List<QuestionInput> questions) {
    }
}
