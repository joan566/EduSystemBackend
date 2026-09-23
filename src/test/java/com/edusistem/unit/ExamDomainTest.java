package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.entity.ExamAnswer;
import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamQuestionOption;
import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
import com.edusistem.core.exam.domain.service.ExamScorer;
import com.edusistem.core.exam.domain.vo.QrPayload;
import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExamDomainTest {

    static ExamQuestion question(int number, String correct, String points) {
        List<ExamQuestionOption> options = new ArrayList<>();
        for (String letter : List.of("A", "B", "C", "D")) {
            options.add(ExamQuestionOption.builder().optionLetter(letter).optionText("Opcion " + letter).build());
        }
        return ExamQuestion.builder().id((long) number).questionNumber(number).statement("Pregunta " + number)
                .correctOption(correct).points(new BigDecimal(points)).options(options).build();
    }

    @Test
    void replaceQuestionsAcceptsAValidExam() {
        Exam exam = new Exam();
        exam.replaceQuestions(List.of(question(2, "C", "1"), question(1, "B", "1")));
        assertThat(exam.getNumberOfQuestions()).isEqualTo(2);
        assertThat(exam.getQuestions().get(0).getQuestionNumber()).isEqualTo(1);
        assertThat(exam.optionCount()).isEqualTo(4);
        assertThat(exam.totalPoints()).isEqualByComparingTo("2");
        assertThat(exam.isReady()).isTrue();
    }

    @Test
    void replaceQuestionsRejectsGapsBadCorrectOptionAndBadPoints() {
        Exam exam = new Exam();
        assertThatThrownBy(() -> exam.replaceQuestions(List.of(question(1, "A", "1"), question(3, "A", "1"))))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("without gaps");
        assertThatThrownBy(() -> exam.replaceQuestions(List.of(question(1, "E", "1"))))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("correctOption");
        assertThatThrownBy(() -> exam.replaceQuestions(List.of(question(1, "A", "0"))))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("points");
    }

    @Test
    void scoreSumsPointsOfCorrectAnswersOnly() {
        List<ExamQuestion> questions = List.of(question(1, "B", "1"), question(2, "C", "2"), question(3, "A", "1"),
                question(4, "D", "1"), question(5, "A", "1"));
        List<ExamAnswer> answers = List.of(
                answer(1L, "B", AnswerDetectionStatus.MARKED),          // correcta: +1
                answer(2L, "A", AnswerDetectionStatus.MARKED),          // incorrecta
                answer(3L, null, AnswerDetectionStatus.EMPTY),          // vacía (no se marca incorrecta)
                answer(4L, null, AnswerDetectionStatus.MULTIPLE_MARK),  // no evaluable
                answer(5L, "A", AnswerDetectionStatus.MANUAL));         // el profesor confirmó: +1
        BigDecimal score = ExamScorer.score(questions, answers);
        assertThat(score).isEqualByComparingTo("2");
        assertThat(answers.get(0).getCorrect()).isTrue();
        assertThat(answers.get(1).getCorrect()).isFalse();
        assertThat(answers.get(2).getCorrect()).isNull();
        assertThat(answers.get(3).getCorrect()).isNull();
        assertThat(answers.get(4).getCorrect()).isTrue();
    }

    private static ExamAnswer answer(Long questionId, String option, AnswerDetectionStatus status) {
        return ExamAnswer.builder().questionId(questionId).selectedOption(option).detectionStatus(status).build();
    }

    @Test
    void qrPayloadRoundTripsAndRejectsGarbage() {
        String encoded = QrPayload.encode(42, "EST-000007");
        QrPayload parsed = QrPayload.parse(encoded);
        assertThat(parsed.examId()).isEqualTo(42);
        assertThat(parsed.studentCode()).isEqualTo("EST-000007");
        assertThat(parsed.version()).isEqualTo("EDU1");
        for (String bad : new String[]{"", "hello", "EDU2|1|X", "EDU1|abc|X", "EDU1|1|", "EDU1|1", null}) {
            assertThatThrownBy(() -> QrPayload.parse(bad)).isInstanceOf(BusinessRuleException.class);
        }
    }
}
