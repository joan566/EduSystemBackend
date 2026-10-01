package com.edusistem.core.exam.infrastructure.adapter.pdf;

import com.edusistem.core.exam.domain.entity.ExamQuestion;
import com.edusistem.core.exam.domain.entity.ExamQuestionOption;
import com.edusistem.core.exam.domain.vo.QuestionBookletData;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Escribe el cuadernillo de preguntas (enunciados y opciones, sin la respuesta correcta) en un documento PDF.
 * El texto de las preguntas usa Liberation Sans embebida (incluida en PDFBox), que cubre tildes, griego y símbolos
 * matemáticos habituales (π, √, ≤, ≥, ±, °…); si no estuviera disponible se usa Helvetica.
 */
final class QuestionBookletWriter {

    private static final float PAGE_W = 595;
    private static final float PAGE_H = 842;
    private static final float LEFT = 52;
    private static final float RIGHT = 543;
    /** Última línea base utilizable (deja sitio al pie de página). */
    private static final float BOTTOM = 792;
    private static final float CONTINUATION_TOP = 78;

    private static final float BODY_SIZE = 10;
    private static final float LEADING = 13.5f;
    private static final float STATEMENT_X = LEFT + 24;
    private static final float OPTION_INDENT = 18;
    private static final float OPTION_BUBBLE_R = 5.2f;
    private static final float QUESTION_GAP = 13;

    private static final float INK = 0.12f;
    private static final float MUTED = 0.45f;
    private static final float RULE = 0.72f;
    private static final float TINT = 0.94f;

    private static final String BODY_FONT = "/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf";

    private final PDFont body;
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /** Algo que se dibuja con su línea base en {@code yTop} (medido desde arriba). */
    private interface Piece {
        void draw(PDPageContentStream cs, float yTop) throws IOException;
    }

    /** Fila de texto: espacio vertical que ocupa antes de su línea base y lo que se dibuja en ella. */
    private record Row(float advance, List<Piece> pieces) {
    }

    QuestionBookletWriter(PDDocument document) {
        this.body = loadBodyFont(document);
    }

    void write(PDDocument document, QuestionBookletData data) throws IOException {
        List<PDPage> pages = new ArrayList<>();
        PDPage page = newPage(document, pages);
        PDPageContentStream cs = new PDPageContentStream(document, page);
        try {
            float y = drawFirstHeader(cs, data);
            for (ExamQuestion question : sorted(data.questions())) {
                List<Row> rows = questionRows(question);
                float height = (float) rows.stream().mapToDouble(Row::advance).sum();
                // Una pregunta no se parte entre páginas salvo que no quepa ni en una página entera.
                if (y + height > BOTTOM && height <= BOTTOM - CONTINUATION_TOP) {
                    cs.close();
                    cs = new PDPageContentStream(document, newPage(document, pages));
                    y = drawContinuationHeader(cs, data);
                }
                for (Row row : rows) {
                    if (y + row.advance() > BOTTOM) {
                        cs.close();
                        cs = new PDPageContentStream(document, newPage(document, pages));
                        y = drawContinuationHeader(cs, data);
                    }
                    y += row.advance();
                    for (Piece piece : row.pieces()) {
                        piece.draw(cs, y);
                    }
                }
                y += QUESTION_GAP;
            }
        } finally {
            cs.close();
        }
        for (int i = 0; i < pages.size(); i++) {
            try (PDPageContentStream footer = new PDPageContentStream(document, pages.get(i),
                    PDPageContentStream.AppendMode.APPEND, true)) {
                centered(footer, body, 7, MUTED, PAGE_W / 2, 818, "Página " + (i + 1) + " de " + pages.size());
            }
        }
    }

    // ---------------------------------------------------------------- cabeceras

    private float drawFirstHeader(PDPageContentStream cs, QuestionBookletData data) throws IOException {
        text(cs, bold, 8, MUTED, LEFT, 60, "CUADERNILLO DE PREGUNTAS");
        float y = 60;
        for (String line : wrap(data.examName(), bold, 16, RIGHT - LEFT)) {
            y += 20;
            text(cs, bold, 16, INK, LEFT, y, line);
        }
        y += 15;
        int count = data.questions().size();
        text(cs, body, 9.5f, MUTED, LEFT, y, String.join("  ·  ", data.subjectName(), data.groupName(),
                count + (count == 1 ? " pregunta" : " preguntas")));

        float boxTop = y + 12;
        List<String> note = wrap("Lea cada pregunta y marque su respuesta en la hoja de respuestas rellenando el "
                + "círculo correspondiente. Este cuadernillo no se califica.", body, 8, RIGHT - LEFT - 20);
        float boxHeight = 14 + note.size() * 10.5f;
        cs.setNonStrokingColor(TINT);
        cs.addRect(LEFT, PAGE_H - boxTop - boxHeight, RIGHT - LEFT, boxHeight);
        cs.fill();
        for (int i = 0; i < note.size(); i++) {
            text(cs, body, 8, INK, LEFT + 10, boxTop + 15 + i * 10.5f, note.get(i));
        }
        return boxTop + boxHeight + 10;
    }

    private float drawContinuationHeader(PDPageContentStream cs, QuestionBookletData data) throws IOException {
        String right = "Cuadernillo de preguntas";
        float rightWidth = PdfPrimitives.width(body, 7.5f, right);
        text(cs, body, 7.5f, MUTED, LEFT, 44, PdfPrimitives.fit(data.examName(), body, 7.5f, RIGHT - LEFT - rightWidth - 20));
        text(cs, body, 7.5f, MUTED, RIGHT - rightWidth, 44, right);
        cs.setStrokingColor(RULE);
        cs.setLineWidth(0.5f);
        cs.moveTo(LEFT, PAGE_H - 51);
        cs.lineTo(RIGHT, PAGE_H - 51);
        cs.stroke();
        return CONTINUATION_TOP - LEADING;
    }

    // ---------------------------------------------------------------- preguntas

    private List<Row> questionRows(ExamQuestion question) throws IOException {
        List<Row> rows = new ArrayList<>();
        List<String> statement = wrap(question.getStatement(), body, BODY_SIZE, RIGHT - STATEMENT_X);
        String number = question.getQuestionNumber() + ".";
        for (int i = 0; i < statement.size(); i++) {
            String line = statement.get(i);
            List<Piece> pieces = new ArrayList<>();
            if (i == 0) {
                pieces.add((cs, y) -> text(cs, bold, BODY_SIZE, INK, STATEMENT_X - 6 - PdfPrimitives.width(bold, BODY_SIZE, number), y,
                        number));
            }
            pieces.add((cs, y) -> text(cs, body, BODY_SIZE, INK, STATEMENT_X, y, line));
            rows.add(new Row(LEADING, pieces));
        }

        List<ExamQuestionOption> options = question.getOptions().stream()
                .sorted(Comparator.comparing(ExamQuestionOption::getOptionLetter)).toList();
        int columns = optionColumns(options);
        if (columns > 1) {
            // Opciones cortas: en una fila o en dos columnas, para ahorrar papel.
            float columnWidth = (RIGHT - STATEMENT_X) / columns;
            for (int start = 0; start < options.size(); start += columns) {
                List<Piece> pieces = new ArrayList<>();
                for (int c = 0; c < columns && start + c < options.size(); c++) {
                    ExamQuestionOption option = options.get(start + c);
                    float x = STATEMENT_X + c * columnWidth;
                    String line = option.getOptionText().strip();
                    pieces.add((cs, y) -> optionBubble(cs, x, y, option.getOptionLetter()));
                    pieces.add((cs, y) -> text(cs, body, BODY_SIZE, INK, x + OPTION_INDENT, y, line));
                }
                rows.add(new Row((start == 0 ? 5 : 3) + LEADING, pieces));
            }
            return rows;
        }
        float gapBefore = 5;
        for (ExamQuestionOption option : options) {
            List<String> lines = wrap(option.getOptionText(), body, BODY_SIZE, RIGHT - STATEMENT_X - OPTION_INDENT);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                List<Piece> pieces = new ArrayList<>();
                if (i == 0) {
                    String letter = option.getOptionLetter();
                    pieces.add((cs, y) -> optionBubble(cs, STATEMENT_X, y, letter));
                }
                pieces.add((cs, y) -> text(cs, body, BODY_SIZE, INK, STATEMENT_X + OPTION_INDENT, y, line));
                rows.add(new Row((i == 0 ? gapBefore : 0) + LEADING, pieces));
            }
            gapBefore = 3;
        }
        return rows;
    }

    /** Todas las opciones en una fila si caben; si no, en dos columnas; si tampoco, una debajo de otra (1). */
    private int optionColumns(List<ExamQuestionOption> options) throws IOException {
        float widest = 0;
        for (ExamQuestionOption option : options) {
            String text = option.getOptionText().strip();
            if (text.contains("\n")) {
                return 1;
            }
            widest = Math.max(widest, PdfPrimitives.width(body, BODY_SIZE, text));
        }
        float needed = OPTION_INDENT + widest + 22;
        float available = RIGHT - STATEMENT_X;
        if (needed * options.size() <= available) {
            return options.size();
        }
        return options.size() > 2 && needed * 2 <= available ? 2 : 1;
    }

    /** La letra de la opción dentro de un círculo, igual que en la hoja de respuestas. */
    private void optionBubble(PDPageContentStream cs, float x, float baseline, String letter) throws IOException {
        float cx = x + OPTION_BUBBLE_R;
        float cy = baseline - 3.4f;
        cs.setStrokingColor(0.35f);
        cs.setLineWidth(0.7f);
        PdfPrimitives.circle(cs, cx, PAGE_H - cy, OPTION_BUBBLE_R);
        cs.stroke();
        centered(cs, bold, 6.5f, INK, cx, cy + 2.3f, letter);
    }

    // ---------------------------------------------------------------- utilidades

    private static List<ExamQuestion> sorted(List<ExamQuestion> questions) {
        return questions.stream().sorted(Comparator.comparingInt(ExamQuestion::getQuestionNumber)).toList();
    }

    private static PDPage newPage(PDDocument document, List<PDPage> pages) {
        PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
        document.addPage(page);
        pages.add(page);
        return page;
    }

    /** Parte el texto en líneas que caben en {@code maxWidth}; respeta los saltos de línea y corta palabras larguísimas. */
    private static List<String> wrap(String value, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : (value == null ? "" : value).split("\\R")) {
            StringBuilder line = new StringBuilder();
            for (String word : PdfPrimitives.sanitize(font, paragraph).strip().split(" +")) {
                if (word.isEmpty()) {
                    continue;
                }
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (PdfPrimitives.width(font, size, candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                while (word.length() > 1 && PdfPrimitives.width(font, size, word) > maxWidth) {
                    int cut = word.length() - 1;
                    while (cut > 1 && PdfPrimitives.width(font, size, word.substring(0, cut)) > maxWidth) {
                        cut--;
                    }
                    lines.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                line.append(word);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static void text(PDPageContentStream cs, PDFont font, float size, float gray, float x, float yTop,
                             String value) throws IOException {
        PdfPrimitives.text(cs, font, size, gray, x, PAGE_H - yTop, value);
    }

    private static void centered(PDPageContentStream cs, PDFont font, float size, float gray, float centerX, float yTop,
                                 String value) throws IOException {
        text(cs, font, size, gray, centerX - PdfPrimitives.width(font, size, value) / 2, yTop, value);
    }

    private static PDFont loadBodyFont(PDDocument document) {
        try (InputStream in = QuestionBookletWriter.class.getResourceAsStream(BODY_FONT)) {
            if (in == null) {
                return new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            }
            return PDType0Font.load(document, in, true);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load the question booklet font", e);
        }
    }
}
