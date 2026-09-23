package com.edusistem.core.exam.domain.service;

import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.BubbleReading;
import com.edusistem.core.exam.domain.vo.DetectedAnswer;
import com.edusistem.core.exam.domain.vo.OmrThresholds;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Decide, a partir del relleno medido de cada burbuja, qué respondió el estudiante:
 * una marca clara → MARKED; ninguna → EMPTY (no se marca incorrecta); varias → MULTIPLE_MARK (no se elige una);
 * marca débil o dudosa → REVIEW_REQUIRED (se sugiere la mejor candidata para que el profesor la revise).
 */
public final class BubbleClassifier {

    /** Relleno a partir del cual una marca se considera plenamente segura. */
    private static final double FULL_MARK = 0.60;

    private final OmrThresholds thresholds;

    public BubbleClassifier(OmrThresholds thresholds) {
        this.thresholds = thresholds;
    }

    public List<DetectedAnswer> classify(BubbleReading reading) {
        List<DetectedAnswer> answers = new ArrayList<>();
        for (int q = 0; q < reading.fillRatios().length; q++) {
            answers.add(classifyQuestion(q + 1, reading.fillRatios()[q]));
        }
        return answers;
    }

    DetectedAnswer classifyQuestion(int questionNumber, double[] fills) {
        int top = 0;
        for (int i = 1; i < fills.length; i++) {
            if (fills[i] > fills[top]) {
                top = i;
            }
        }
        double second = 0;
        for (int i = 0; i < fills.length; i++) {
            if (i != top) {
                second = Math.max(second, fills[i]);
            }
        }
        double best = fills[top];
        String letter = String.valueOf(AnswerSheetLayout.optionLetter(top));

        if (best < thresholds.ambiguityThreshold()) {
            return answer(questionNumber, null, AnswerDetectionStatus.EMPTY, 1 - best / thresholds.ambiguityThreshold());
        }
        if (best < thresholds.markThreshold()) {
            return answer(questionNumber, letter, AnswerDetectionStatus.REVIEW_REQUIRED,
                    Math.min(best / thresholds.markThreshold(), thresholds.reviewConfidence() - 0.01));
        }
        if (second >= thresholds.markThreshold()) {
            return answer(questionNumber, null, AnswerDetectionStatus.MULTIPLE_MARK, Math.min(1, second / FULL_MARK));
        }
        double confidence = Math.min(1, best / FULL_MARK) * (1 - second / best);
        AnswerDetectionStatus status = confidence >= thresholds.reviewConfidence()
                ? AnswerDetectionStatus.MARKED : AnswerDetectionStatus.REVIEW_REQUIRED;
        return answer(questionNumber, letter, status, confidence);
    }

    private static DetectedAnswer answer(int number, String option, AnswerDetectionStatus status, double confidence) {
        BigDecimal c = BigDecimal.valueOf(Math.max(0, Math.min(1, confidence))).setScale(4, RoundingMode.HALF_UP);
        return new DetectedAnswer(number, option, status, c);
    }
}
