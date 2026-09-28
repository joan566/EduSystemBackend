package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.exam.application.use_case.dtos.ExamCommands;
import com.edusistem.core.exam.domain.entity.Exam;
import com.edusistem.core.exam.domain.vo.DocumentParagraph;
import com.edusistem.core.exam.domain.vo.DocumentParagraph.ListKind;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte los párrafos de un documento Word en preguntas. Todo lo que hay antes de la pregunta 1 (título,
 * instrucciones) se ignora. Formato:
 * <pre>
 * 1. Enunciado (puede ocupar varios párrafos)
 * A) Opción
 * B) Opción
 * Respuesta: B
 * Puntos: 2            (opcional)
 * </pre>
 * También acepta "1)" o "Pregunta 1:", opciones "a." o "A -", la opción correcta marcada con "*" en lugar de la línea
 * "Respuesta:" y las listas automáticas de Word (numeradas para las preguntas, con letras para las opciones).
 * Reúne todos los problemas del documento y los informa juntos.
 */
public final class QuestionDocumentParser {

    private static final Pattern QUESTION =
            Pattern.compile("(?:pregunta\\s*)?(\\d{1,3})\\s*(?:[.:\\-]|\\))(?:\\s+|$)(.*)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OPTION =
            Pattern.compile("(\\*\\s*)?([a-z])\\s*[.):\\-]\\s+(.*?)(\\s*\\*)?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ANSWER =
            Pattern.compile("(?:respuesta(?:\\s+correcta)?|clave|correcta)\\s*[:=\\-]?\\s*\\(?([a-z])\\)?\\s*\\.?",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern POINTS =
            Pattern.compile("(?:puntos?|puntaje|valor)\\s*[:=\\-]?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:pts?\\.?|puntos?)?",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKER = Pattern.compile("^\\*\\s*|\\s*\\*$");

    private static final class Draft {
        final int number;
        final int paragraph;
        final StringBuilder statement = new StringBuilder();
        final List<String> options = new ArrayList<>();
        String correct;
        BigDecimal points;

        Draft(int number, int paragraph) {
            this.number = number;
            this.paragraph = paragraph;
        }

        String nextLetter() {
            return String.valueOf((char) ('A' + options.size()));
        }

        void appendStatement(String text) {
            if (!statement.isEmpty()) {
                statement.append('\n');
            }
            statement.append(text);
        }
    }

    public List<ExamCommands.QuestionInput> parse(List<DocumentParagraph> paragraphs) {
        List<Draft> drafts = new ArrayList<>();
        List<InvalidRequestException.Detail> errors = new ArrayList<>();
        Draft current = null;
        for (int i = 0; i < paragraphs.size(); i++) {
            DocumentParagraph paragraph = paragraphs.get(i);
            String text = clean(paragraph.text());
            int line = i + 1;
            if (text.isEmpty()) {
                continue;
            }
            int expected = drafts.size() + 1;

            Matcher question = QUESTION.matcher(text);
            boolean numberedList = paragraph.listKind() == ListKind.NUMBERED;
            boolean typedNumber = paragraph.listKind() == ListKind.NONE && question.matches();
            if (numberedList || typedNumber && Integer.parseInt(question.group(1)) == expected) {
                current = new Draft(expected, line);
                drafts.add(current);
                String statement = numberedList ? text : question.group(2).strip();
                if (!statement.isEmpty()) {
                    current.appendStatement(statement);
                }
                continue;
            }
            if (current == null) {
                continue; // título e instrucciones antes de la primera pregunta
            }
            if (typedNumber && !current.options.isEmpty()) {
                errors.add(at(line, "Expected question " + expected + " but found question " + question.group(1)));
                continue;
            }

            Matcher answer = ANSWER.matcher(text);
            if (answer.matches()) {
                String letter = answer.group(1).toUpperCase();
                if (current.correct != null && !current.correct.equals(letter)) {
                    errors.add(of(current, "has more than one correct answer (" + current.correct + " and " + letter + ")"));
                }
                current.correct = letter;
                continue;
            }
            Matcher points = POINTS.matcher(text);
            if (points.matches()) {
                current.points = new BigDecimal(points.group(1).replace(',', '.'));
                continue;
            }

            if (paragraph.listKind() == ListKind.LETTERED) {
                addOption(current, current.nextLetter(), text, errors, line);
                continue;
            }
            Matcher option = OPTION.matcher(text);
            // Antes de la primera opción, "I. …" o "C. …" son parte del enunciado (p. ej. afirmaciones numeradas).
            if (option.matches() && (!current.options.isEmpty() || option.group(2).equalsIgnoreCase("A"))) {
                String letter = option.group(2).toUpperCase();
                if (!letter.equals(current.nextLetter())) {
                    errors.add(at(line, "Question " + current.number + ": expected option " + current.nextLetter()
                            + " but found " + letter));
                    continue;
                }
                String body = option.group(3).strip();
                addOption(current, letter, (option.group(1) != null || option.group(4) != null) ? "*" + body : body,
                        errors, line);
                continue;
            }

            if (current.options.isEmpty()) {
                current.appendStatement(text);
            } else {
                errors.add(at(line, "Question " + current.number + ": unexpected text after the options (\""
                        + abbreviate(text) + "\"); expected another option, \"Respuesta: X\" or the next question"));
            }
        }
        validate(drafts, errors);
        if (!errors.isEmpty()) {
            throw new InvalidRequestException("INVALID_QUESTION_DOCUMENT",
                    "The document has " + errors.size() + " problem(s); fix them and upload it again", errors);
        }
        return drafts.stream().map(d -> new ExamCommands.QuestionInput(d.number, d.statement.toString(), d.correct,
                d.points, optionInputs(d))).toList();
    }

    /** Agrega la opción; un "*" al principio o al final la marca como correcta. */
    private static void addOption(Draft draft, String letter, String text, List<InvalidRequestException.Detail> errors,
                                  int line) {
        String body = MARKER.matcher(text).replaceAll("").strip();
        if (!body.equals(text.strip())) {
            if (draft.correct != null && !draft.correct.equals(letter)) {
                errors.add(of(draft, "has more than one correct answer (" + draft.correct + " and " + letter + ")"));
            }
            draft.correct = letter;
        }
        if (draft.options.size() == Exam.MAX_OPTIONS) {
            errors.add(at(line, "Question " + draft.number + ": at most " + Exam.MAX_OPTIONS + " options are allowed"));
            return;
        }
        draft.options.add(body);
    }

    private static void validate(List<Draft> drafts, List<InvalidRequestException.Detail> errors) {
        if (drafts.isEmpty()) {
            errors.add(new InvalidRequestException.Detail("document",
                    "No questions were found; number them \"1.\", \"2.\"… as in the template"));
            return;
        }
        if (drafts.size() > Exam.MAX_QUESTIONS) {
            errors.add(new InvalidRequestException.Detail("document",
                    "The document has " + drafts.size() + " questions; the maximum is " + Exam.MAX_QUESTIONS));
        }
        int optionCount = drafts.get(0).options.size();
        for (Draft d : drafts) {
            if (d.statement.toString().isBlank()) {
                errors.add(of(d, "has no statement"));
            }
            if (d.options.size() < Exam.MIN_OPTIONS) {
                errors.add(of(d, "needs at least " + Exam.MIN_OPTIONS + " options (A), B), …)"));
            } else if (d.options.size() != optionCount) {
                errors.add(of(d, "has " + d.options.size() + " options but question 1 has " + optionCount
                        + "; all questions must have the same number of options"));
            }
            d.options.stream().filter(String::isBlank).findFirst()
                    .ifPresent(o -> errors.add(of(d, "has an option without text")));
            if (d.correct == null) {
                errors.add(of(d, "has no correct answer; add \"Respuesta: X\" or mark the option with *"));
            } else if (d.correct.charAt(0) - 'A' >= d.options.size()) {
                errors.add(of(d, "the correct answer " + d.correct + " is not one of its options"));
            }
            if (d.points != null && d.points.signum() <= 0) {
                errors.add(of(d, "points must be greater than zero"));
            }
        }
    }

    private static List<ExamCommands.OptionInput> optionInputs(Draft draft) {
        List<ExamCommands.OptionInput> options = new ArrayList<>();
        for (int o = 0; o < draft.options.size(); o++) {
            options.add(new ExamCommands.OptionInput(String.valueOf((char) ('A' + o)), draft.options.get(o)));
        }
        return options;
    }

    private static InvalidRequestException.Detail of(Draft draft, String message) {
        return new InvalidRequestException.Detail("question " + draft.number,
                "Question " + draft.number + " (paragraph " + draft.paragraph + ") " + message);
    }

    private static InvalidRequestException.Detail at(int paragraph, String message) {
        return new InvalidRequestException.Detail("paragraph " + paragraph, message);
    }

    /** Normaliza espacios (incluidos los no separables y tabulaciones) conservando los saltos de línea manuales. */
    private static String clean(String text) {
        return text.replace(' ', ' ').replace('\t', ' ').replaceAll(" {2,}", " ")
                .replaceAll(" *\\R *", "\n").strip();
    }

    private static String abbreviate(String text) {
        String single = text.replace('\n', ' ');
        return single.length() <= 60 ? single : single.substring(0, 57) + "...";
    }
}
