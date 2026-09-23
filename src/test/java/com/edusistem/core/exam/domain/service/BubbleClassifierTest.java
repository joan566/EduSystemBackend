package com.edusistem.core.exam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
import com.edusistem.core.exam.domain.vo.BubbleReading;
import com.edusistem.core.exam.domain.vo.DetectedAnswer;
import com.edusistem.core.exam.domain.vo.OmrThresholds;
import java.util.List;
import org.junit.jupiter.api.Test;

class BubbleClassifierTest {

    private final BubbleClassifier classifier = new BubbleClassifier(OmrThresholds.defaults());

    @Test
    void oneClearMarkIsAcceptedWithHighConfidence() {
        DetectedAnswer a = classifier.classifyQuestion(1, new double[]{0.02, 0.93, 0.01, 0.0});
        assertThat(a.status()).isEqualTo(AnswerDetectionStatus.MARKED);
        assertThat(a.selectedOption()).isEqualTo("B");
        assertThat(a.confidence().doubleValue()).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    void noMarkIsEmptyNotWrong() {
        DetectedAnswer a = classifier.classifyQuestion(2, new double[]{0.02, 0.0, 0.05, 0.01});
        assertThat(a.status()).isEqualTo(AnswerDetectionStatus.EMPTY);
        assertThat(a.selectedOption()).isNull();
    }

    @Test
    void severalMarksAreMultipleMarkAndNoOptionIsChosen() {
        DetectedAnswer a = classifier.classifyQuestion(3, new double[]{0.9, 0.02, 0.85, 0.0});
        assertThat(a.status()).isEqualTo(AnswerDetectionStatus.MULTIPLE_MARK);
        assertThat(a.selectedOption()).isNull();
    }

    @Test
    void weakMarkRequiresReviewButSuggestsTheCandidate() {
        DetectedAnswer light = classifier.classifyQuestion(4, new double[]{0.0, 0.0, 0.50, 0.0});
        assertThat(light.status()).isEqualTo(AnswerDetectionStatus.REVIEW_REQUIRED);
        assertThat(light.selectedOption()).isEqualTo("C");
        DetectedAnswer faint = classifier.classifyQuestion(5, new double[]{0.30, 0.0, 0.0, 0.0});
        assertThat(faint.status()).isEqualTo(AnswerDetectionStatus.REVIEW_REQUIRED);
        assertThat(faint.selectedOption()).isEqualTo("A");
    }

    @Test
    void markWithSmudgeElsewhereRequiresReview() {
        DetectedAnswer a = classifier.classifyQuestion(6, new double[]{0.9, 0.3, 0.0, 0.0});
        assertThat(a.status()).isEqualTo(AnswerDetectionStatus.REVIEW_REQUIRED);
    }

    @Test
    void classifyProcessesEveryQuestionInOrder() {
        List<DetectedAnswer> answers = classifier.classify(new BubbleReading(new double[][]{
                {0.9, 0, 0, 0}, {0, 0, 0, 0}, {0, 0.9, 0.9, 0}}));
        assertThat(answers).extracting(DetectedAnswer::questionNumber).containsExactly(1, 2, 3);
        assertThat(answers).extracting(DetectedAnswer::status).containsExactly(AnswerDetectionStatus.MARKED,
                AnswerDetectionStatus.EMPTY, AnswerDetectionStatus.MULTIPLE_MARK);
    }
}
