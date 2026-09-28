package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.edusistem.core.exam.application.use_case.dtos.ExamCommands.OptionInput;
import com.edusistem.core.exam.application.use_case.dtos.ExamCommands.QuestionInput;
import com.edusistem.core.exam.application.use_case.service.QuestionDocumentParser;
import com.edusistem.core.exam.domain.vo.DocumentParagraph;
import com.edusistem.core.exam.domain.vo.DocumentParagraph.ListKind;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionDocumentParserTest {

    private final QuestionDocumentParser parser = new QuestionDocumentParser();

    private static List<DocumentParagraph> lines(String... text) {
        return Arrays.stream(text).map(t -> new DocumentParagraph(t, ListKind.NONE)).toList();
    }

    private InvalidRequestException failure(List<DocumentParagraph> paragraphs) {
        return catchThrowableOfType(InvalidRequestException.class, () -> parser.parse(paragraphs));
    }

    @Test
    void parsesTheTemplateFormatIgnoringTheTitleAndInstructions() {
        List<QuestionInput> questions = parser.parse(lines(
                "Examen de Biología – Grado 10",
                "Instrucciones: responda todas las preguntas.",
                "",
                "1. ¿Cuál es el órgano que bombea la sangre?",
                "A) Pulmón",
                "B) Corazón",
                "C) Hígado",
                "D) Riñón",
                "Respuesta: B",
                "",
                "2) La fórmula del agua es",
                "a. CO2",
                "b. H2O",
                "c. O2",
                "d. NaCl",
                "respuesta correcta: b",
                "Puntos: 1,5"));
        assertThat(questions).hasSize(2);
        QuestionInput first = questions.get(0);
        assertThat(first.questionNumber()).isEqualTo(1);
        assertThat(first.statement()).isEqualTo("¿Cuál es el órgano que bombea la sangre?");
        assertThat(first.correctOption()).isEqualTo("B");
        assertThat(first.points()).isNull();
        assertThat(first.options()).extracting(OptionInput::letter).containsExactly("A", "B", "C", "D");
        assertThat(first.options()).extracting(OptionInput::text).containsExactly("Pulmón", "Corazón", "Hígado", "Riñón");
        assertThat(questions.get(1).correctOption()).isEqualTo("B");
        assertThat(questions.get(1).points()).isEqualByComparingTo("1.5");
    }

    @Test
    void acceptsTheCorrectOptionMarkedWithAnAsterisk() {
        List<QuestionInput> questions = parser.parse(lines(
                "Pregunta 1: 2 + 2 =",
                "A) 3",
                "*B) 4",
                "C) 5",
                "Pregunta 2: 3 × 3 =",
                "A) 6",
                "B) 8",
                "C) 9 *"));
        assertThat(questions).extracting(QuestionInput::correctOption).containsExactly("B", "C");
        assertThat(questions.get(0).options().get(1).text()).isEqualTo("4");
        assertThat(questions.get(1).options().get(2).text()).isEqualTo("9");
    }

    @Test
    void understandsWordAutomaticListsForQuestionsAndOptions() {
        List<QuestionInput> questions = parser.parse(List.of(
                new DocumentParagraph("Título", ListKind.NONE),
                new DocumentParagraph("¿Capital de Colombia?", ListKind.NUMBERED),
                new DocumentParagraph("Medellín", ListKind.LETTERED),
                new DocumentParagraph("Bogotá", ListKind.LETTERED),
                new DocumentParagraph("Respuesta: B", ListKind.NONE),
                new DocumentParagraph("¿Río más largo de Colombia?", ListKind.NUMBERED),
                new DocumentParagraph("Magdalena*", ListKind.LETTERED),
                new DocumentParagraph("Cauca", ListKind.LETTERED)));
        assertThat(questions).hasSize(2);
        assertThat(questions.get(1).questionNumber()).isEqualTo(2);
        assertThat(questions.get(1).options()).extracting(OptionInput::text).containsExactly("Magdalena", "Cauca");
        assertThat(questions.get(1).correctOption()).isEqualTo("A");
    }

    @Test
    void keepsMultiParagraphStatementsIncludingNumberedAffirmations() {
        List<QuestionInput> questions = parser.parse(lines(
                "1. Lea las afirmaciones:",
                "I. El agua hierve a 100 °C al nivel del mar.",
                "II. El hielo es más denso que el agua.",
                "¿Cuáles son verdaderas?",
                "A) Solo I",
                "B) Solo II",
                "C) I y II",
                "Respuesta: A"));
        assertThat(questions.get(0).statement()).isEqualTo("Lea las afirmaciones:\n"
                + "I. El agua hierve a 100 °C al nivel del mar.\nII. El hielo es más denso que el agua.\n"
                + "¿Cuáles son verdaderas?");
    }

    @Test
    void reportsEveryProblemOfTheDocumentAtOnce() {
        InvalidRequestException error = failure(lines(
                "1. Primera",
                "A) Uno",
                "B) Dos",
                "C) Tres",
                "2. Segunda",
                "A) Uno",
                "B) Dos",
                "Respuesta: A",
                "3. Tercera",
                "A) Uno",
                "C) Dos",
                "Respuesta: E",
                "Nota del profesor"));
        assertThat(error.getCode()).isEqualTo("INVALID_QUESTION_DOCUMENT");
        assertThat(error.getDetails()).extracting(InvalidRequestException.Detail::message).anySatisfy(m ->
                assertThat(m).contains("Question 1").contains("no correct answer"))
                .anySatisfy(m -> assertThat(m).contains("Question 2").contains("has 2 options but question 1 has 3"))
                .anySatisfy(m -> assertThat(m).contains("expected option B but found C"))
                .anySatisfy(m -> assertThat(m).contains("correct answer E is not one of its options"))
                .anySatisfy(m -> assertThat(m).contains("unexpected text after the options"));
    }

    @Test
    void failsWhenNoQuestionIsFoundOrNumbersSkip() {
        assertThat(failure(lines("Solo un título", "Y un párrafo")).getDetails())
                .extracting(InvalidRequestException.Detail::message).anySatisfy(m -> assertThat(m).contains("No questions"));
        assertThat(failure(lines("1. Uno", "A) a", "B) b", "Respuesta: A", "3. Tres", "A) a", "B) b", "Respuesta: B"))
                .getDetails()).extracting(InvalidRequestException.Detail::message)
                .anySatisfy(m -> assertThat(m).contains("Expected question 2 but found question 3"));
    }
}
