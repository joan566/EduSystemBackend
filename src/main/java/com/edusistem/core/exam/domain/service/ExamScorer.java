package com.edusistem.core.exam.domain.service;

import com.edusistem.core.exam.domain.entity.ExamAnswer;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Calcula la puntuación bruta: suma de los puntos de las preguntas respondidas correctamente. */
public final class ExamScorer {

    private ExamScorer() {
    }

    /** Fija {@code correct} en cada respuesta evaluable y devuelve la puntuación bruta. */
    public static BigDecimal score(List<ExamQuestion> questions, List<ExamAnswer> answers) {
        Map<Long, ExamQuestion> byId = questions.stream().collect(Collectors.toMap(ExamQuestion::getId, Function.identity()));
        BigDecimal score = BigDecimal.ZERO;
        for (ExamAnswer answer : answers) {
            ExamQuestion question = byId.get(answer.getQuestionId());
            answer.setCorrect(evaluate(question, answer));
            if (Boolean.TRUE.equals(answer.getCorrect())) {
                score = score.add(question.getPoints());
            }
        }
        return score;
    }

    private static Boolean evaluate(ExamQuestion question, ExamAnswer answer) {
        AnswerDetectionStatus status = answer.getDetectionStatus();
        if (status == AnswerDetectionStatus.MANUAL) {
            // El profesor confirmó la respuesta: vacía = incorrecta confirmada.
            return answer.getSelectedOption() != null && answer.getSelectedOption().equals(question.getCorrectOption());
        }
        if (status == AnswerDetectionStatus.MARKED) {
            return answer.getSelectedOption().equals(question.getCorrectOption());
        }
        return null; // EMPTY, MULTIPLE_MARK, REVIEW_REQUIRED: no evaluable, aporta 0 puntos
    }
}
