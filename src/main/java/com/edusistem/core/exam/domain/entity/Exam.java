package com.edusistem.core.exam.domain.entity;

import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Examen de selección múltiple con única respuesta (estilo ICFES). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    public static final int MAX_QUESTIONS = 100;
    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 6;

    private Long id;
    private Long evaluationId;
    private int numberOfQuestions;
    @Builder.Default
    private List<ExamQuestion> questions = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Las preguntas están completamente definidas y coinciden con la cantidad declarada. */
    public boolean isReady() {
        return !questions.isEmpty() && questions.size() == numberOfQuestions;
    }

    public int optionCount() {
        return questions.isEmpty() ? 0 : questions.get(0).getOptions().size();
    }

    public BigDecimal totalPoints() {
        return questions.stream().map(ExamQuestion::getPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public ExamQuestion questionByNumber(int number) {
        return questions.stream().filter(q -> q.getQuestionNumber() == number).findFirst().orElse(null);
    }

    public static void validateNumberOfQuestions(int numberOfQuestions) {
        if (numberOfQuestions < 1 || numberOfQuestions > MAX_QUESTIONS) {
            throw new InvalidRequestException("INVALID_NUMBER_OF_QUESTIONS",
                    "numberOfQuestions must be between 1 and " + MAX_QUESTIONS);
        }
    }

    /**
     * Valida y asigna las preguntas: numeración 1..n sin huecos, mismas opciones (A, B, C...) en todas, respuesta
     * correcta existente y puntos positivos.
     */
    public void replaceQuestions(List<ExamQuestion> newQuestions) {
        validateNumberOfQuestions(newQuestions.size());
        List<ExamQuestion> sorted = newQuestions.stream().sorted(Comparator.comparingInt(ExamQuestion::getQuestionNumber)).toList();
        int optionCount = sorted.get(0).getOptions().size();
        if (optionCount < MIN_OPTIONS || optionCount > MAX_OPTIONS) {
            throw new InvalidRequestException("INVALID_OPTIONS",
                    "Each question needs between " + MIN_OPTIONS + " and " + MAX_OPTIONS + " options");
        }
        for (int i = 0; i < sorted.size(); i++) {
            ExamQuestion q = sorted.get(i);
            if (q.getQuestionNumber() != i + 1) {
                throw new InvalidRequestException("INVALID_QUESTION_NUMBERING", "Questions must be numbered 1..n without gaps");
            }
            if (q.getStatement() == null || q.getStatement().isBlank()) {
                throw new InvalidRequestException("INVALID_QUESTION", "Question " + q.getQuestionNumber() + " has no statement");
            }
            if (q.getPoints() == null || q.getPoints().signum() <= 0) {
                throw new InvalidRequestException("INVALID_POINTS", "Question " + q.getQuestionNumber() + " needs positive points");
            }
            if (q.getOptions().size() != optionCount) {
                throw new InvalidRequestException("INVALID_OPTIONS", "All questions must have the same number of options");
            }
            Set<String> letters = new HashSet<>();
            for (int o = 0; o < q.getOptions().size(); o++) {
                String expected = String.valueOf((char) ('A' + o));
                ExamQuestionOption option = q.getOptions().get(o);
                if (!expected.equals(option.getOptionLetter()) || !letters.add(option.getOptionLetter())
                        || option.getOptionText() == null || option.getOptionText().isBlank()) {
                    throw new InvalidRequestException("INVALID_OPTIONS",
                            "Question " + q.getQuestionNumber() + ": options must be A, B, C... in order, with text");
                }
            }
            if (!q.hasOption(q.getCorrectOption())) {
                throw new InvalidRequestException("INVALID_CORRECT_OPTION",
                        "Question " + q.getQuestionNumber() + ": correctOption must be one of its options");
            }
        }
        this.questions = new ArrayList<>(sorted);
        this.numberOfQuestions = sorted.size();
    }
}
