package com.edusistem.core.exam.infrastructure.adapter.pdf;

import com.edusistem.core.exam.domain.outputports.AnswerSheetRendererPort;
import com.edusistem.core.exam.domain.vo.AnswerSheetData;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.QuestionBookletData;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Dibuja la hoja con las coordenadas exactas de {@link AnswerSheetLayout} (vectorial y determinista).
 * Restricciones del lector OMR que el diseño respeta: solo los 4 marcadores son cuadrados negros sólidos; alrededor de
 * cada burbuja (hasta 1.7 × radio) solo hay papel; lo que se imprime dentro de la burbuja es gris claro, muy por encima
 * del umbral de "oscuro"; y el QR conserva su zona blanca de silencio.
 */
@Component
public class PdfAnswerSheetRenderer implements AnswerSheetRendererPort {

    private static final double PAGE_H = AnswerSheetLayout.PAGE_HEIGHT;
    private static final float LEFT = (float) AnswerSheetLayout.CONTENT_LEFT;
    private static final float RIGHT = (float) AnswerSheetLayout.CONTENT_RIGHT;

    /** Borde derecho de la cabecera: deja ~20 pt de papel antes del QR (zona de silencio). */
    private static final float HEADER_RIGHT = (float) AnswerSheetLayout.QR_X - 20;
    private static final float INFO_TOP = 100;
    private static final float INFO_MID = 129;
    private static final float INFO_BOTTOM = 158;
    private static final float CODE_RIGHT = LEFT + 105;
    private static final float GROUP_RIGHT = CODE_RIGHT + 90;
    private static final float INSTRUCTIONS_TOP = 180;
    private static final float INSTRUCTIONS_BOTTOM = 222;

    private static final float INK = 0.12f;
    private static final float MUTED = 0.45f;
    private static final float RULE = 0.72f;
    private static final float TINT = 0.94f;
    private static final float BUBBLE_STROKE = 0.25f;
    /** Letra dentro de la burbuja: bastante más clara que el umbral de oscuro del lector (62 % del papel). */
    private static final float BUBBLE_LETTER = 0.74f;

    private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Override
    public byte[] render(List<AnswerSheetData> sheets, QuestionBookletData booklet) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            QuestionBookletWriter bookletWriter = booklet == null ? null : new QuestionBookletWriter(document);
            for (AnswerSheetData sheet : sheets) {
                PDPage page = new PDPage(new PDRectangle((float) AnswerSheetLayout.PAGE_WIDTH, (float) PAGE_H));
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    drawSheet(cs, sheet);
                }
                if (bookletWriter != null) {
                    bookletWriter.write(document, booklet);
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate the answer sheet PDF", e);
        }
    }

    @Override
    public byte[] renderBooklet(QuestionBookletData booklet) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new QuestionBookletWriter(document).write(document, booklet);
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate the question booklet PDF", e);
        }
    }

    private void drawSheet(PDPageContentStream cs, AnswerSheetData sheet) throws IOException {
        AnswerSheetLayout layout = sheet.layout();
        drawMarkers(cs, layout);
        drawQr(cs, sheet.qrContent());
        drawHeader(cs, sheet);
        drawInstructions(cs);
        drawGrid(cs, layout);
        centeredText(cs, regular, 6.5f, MUTED, (float) AnswerSheetLayout.PAGE_WIDTH / 2, 803,
                "EduSistem  ·  Hoja de lectura automática  ·  No doblar ni arrugar");
    }

    private void drawMarkers(PDPageContentStream cs, AnswerSheetLayout layout) throws IOException {
        cs.setNonStrokingColor(0f);
        for (AnswerSheetLayout.Marker m : layout.markers()) {
            cs.addRect((float) (m.centerX() - m.size() / 2), (float) (PAGE_H - m.centerY() - m.size() / 2),
                    (float) m.size(), (float) m.size());
            cs.fill();
        }
    }

    private void drawHeader(PDPageContentStream cs, AnswerSheetData sheet) throws IOException {
        float width = HEADER_RIGHT - LEFT;
        text(cs, bold, 8, MUTED, LEFT, 68, "HOJA DE RESPUESTAS");
        float titleSize = 16;
        while (titleSize > 12 && PdfPrimitives.width(bold, titleSize, sheet.examName()) > width) {
            titleSize -= 0.5f;
        }
        text(cs, bold, titleSize, INK, LEFT, 89, PdfPrimitives.fit(sheet.examName(), bold, titleSize, width));

        cs.setStrokingColor(RULE);
        cs.setLineWidth(0.6f);
        roundedRect(cs, LEFT, INFO_TOP, width, INFO_BOTTOM - INFO_TOP, 4);
        cs.stroke();
        line(cs, LEFT, INFO_MID, HEADER_RIGHT, INFO_MID);
        line(cs, CODE_RIGHT, INFO_MID, CODE_RIGHT, INFO_BOTTOM);
        line(cs, GROUP_RIGHT, INFO_MID, GROUP_RIGHT, INFO_BOTTOM);

        field(cs, LEFT, HEADER_RIGHT, INFO_TOP, "ESTUDIANTE", sheet.studentName(), 11);
        field(cs, LEFT, CODE_RIGHT, INFO_MID, "CÓDIGO", sheet.studentCode(), 10);
        field(cs, CODE_RIGHT, GROUP_RIGHT, INFO_MID, "GRUPO", sheet.groupName(), 10);
        field(cs, GROUP_RIGHT, HEADER_RIGHT, INFO_MID, "ASIGNATURA", sheet.subjectName(), 10);

        float qrCenter = (float) (AnswerSheetLayout.QR_X + AnswerSheetLayout.QR_SIZE / 2);
        centeredText(cs, regular, 6, MUTED, qrCenter, (float) (AnswerSheetLayout.QR_Y + AnswerSheetLayout.QR_SIZE + 14),
                "No escriba sobre el código");
    }

    /** Celda de la ficha del estudiante: etiqueta pequeña en gris y valor debajo. */
    private void field(PDPageContentStream cs, float left, float right, float top, String label, String value,
                       float size) throws IOException {
        text(cs, bold, 6, MUTED, left + 8, top + 10, label);
        text(cs, bold, size, INK, left + 8, top + 23, PdfPrimitives.fit(value, bold, size, right - left - 16));
    }

    private void drawInstructions(PDPageContentStream cs) throws IOException {
        cs.setNonStrokingColor(TINT);
        roundedRect(cs, LEFT, INSTRUCTIONS_TOP, RIGHT - LEFT, INSTRUCTIONS_BOTTOM - INSTRUCTIONS_TOP, 4);
        cs.fill();

        float x = LEFT + 10;
        text(cs, bold, 6, MUTED, x, INSTRUCTIONS_TOP + 11, "INSTRUCCIONES");
        text(cs, regular, 7.5f, INK, x, INSTRUCTIONS_TOP + 23,
                "Use lápiz oscuro o bolígrafo negro y rellene por completo UN solo círculo por pregunta.");
        text(cs, regular, 7.5f, INK, x, INSTRUCTIONS_TOP + 33,
                "Si se equivoca, borre bien. No marque fuera de los círculos ni sobre los cuadros negros.");

        // Ejemplos de marca correcta e incorrectas.
        float exampleY = INSTRUCTIONS_TOP + 28;
        float r = 5;
        float correctX = 404;
        text(cs, bold, 6, MUTED, correctX - r, INSTRUCTIONS_TOP + 11, "CORRECTO");
        exampleBubble(cs, correctX, exampleY, r);
        cs.setNonStrokingColor(INK);
        PdfPrimitives.circle(cs, correctX, PAGE_H - exampleY, r);
        cs.fill();

        float wrongX = 462;
        text(cs, bold, 6, MUTED, wrongX - r, INSTRUCTIONS_TOP + 11, "INCORRECTO");
        cs.setStrokingColor(INK);
        cs.setLineWidth(1.1f);
        // Visto
        exampleBubble(cs, wrongX, exampleY, r);
        cs.setStrokingColor(INK);
        cs.setLineWidth(1.1f);
        polyline(cs, wrongX - 2.8f, exampleY, wrongX - 0.6f, exampleY + 2.4f, wrongX + 3, exampleY - 2.8f);
        // Equis
        float crossX = wrongX + 17;
        exampleBubble(cs, crossX, exampleY, r);
        cs.setStrokingColor(INK);
        cs.setLineWidth(1.1f);
        line(cs, crossX - 2.8f, exampleY - 2.8f, crossX + 2.8f, exampleY + 2.8f);
        line(cs, crossX - 2.8f, exampleY + 2.8f, crossX + 2.8f, exampleY - 2.8f);
        // Punto parcial
        float dotX = crossX + 17;
        exampleBubble(cs, dotX, exampleY, r);
        cs.setNonStrokingColor(INK);
        PdfPrimitives.circle(cs, dotX, PAGE_H - exampleY, 1.8);
        cs.fill();
        // Dos círculos rellenos
        float doubleX = dotX + 17;
        exampleBubble(cs, doubleX, exampleY, r);
        exampleBubble(cs, doubleX + 12, exampleY, r);
        cs.setNonStrokingColor(INK);
        PdfPrimitives.circle(cs, doubleX, PAGE_H - exampleY, r);
        PdfPrimitives.circle(cs, doubleX + 12, PAGE_H - exampleY, r);
        cs.fill();
    }

    private void exampleBubble(PDPageContentStream cs, float x, float yTop, float r) throws IOException {
        cs.setNonStrokingColor(1f);
        PdfPrimitives.circle(cs, x, PAGE_H - yTop, r);
        cs.fill();
        cs.setStrokingColor(BUBBLE_STROKE);
        cs.setLineWidth(0.75f);
        PdfPrimitives.circle(cs, x, PAGE_H - yTop, r);
        cs.stroke();
    }

    private void drawGrid(PDPageContentStream cs, AnswerSheetLayout layout) throws IOException {
        double half = AnswerSheetLayout.ROW_SPACING / 2;
        for (int column = 0; column < layout.columns(); column++) {
            int rows = layout.rowsInColumn(column);
            int firstQuestion = column * layout.rowsPerColumn() + 1;
            float left = (float) layout.columnLeft(column);
            float right = (float) (layout.bubbleCenter(firstQuestion, layout.optionCount() - 1).x()
                    + AnswerSheetLayout.BUBBLE_RADIUS);
            // Separador fino cada 5 preguntas, justo a media fila (fuera del anillo de papel de las burbujas).
            cs.setStrokingColor(RULE);
            cs.setLineWidth(0.4f);
            for (int row = 5; row < rows; row += 5) {
                float y = (float) (AnswerSheetLayout.FIRST_ROW_Y + (row - 1) * AnswerSheetLayout.ROW_SPACING + half);
                line(cs, left + 1, y, right, y);
            }
        }

        double r = AnswerSheetLayout.BUBBLE_RADIUS;
        for (int q = 1; q <= layout.numberOfQuestions(); q++) {
            AnswerSheetLayout.Point first = layout.bubbleCenter(q, 0);
            String number = String.valueOf(q);
            float numberRight = (float) (first.x() - r - 5.5);
            text(cs, bold, 8, INK, numberRight - PdfPrimitives.width(bold, 8, number), (float) first.y() + 2.9f, number);

            cs.setStrokingColor(BUBBLE_STROKE);
            cs.setLineWidth(0.75f);
            for (int o = 0; o < layout.optionCount(); o++) {
                AnswerSheetLayout.Point c = layout.bubbleCenter(q, o);
                PdfPrimitives.circle(cs, c.x(), PAGE_H - c.y(), r);
            }
            cs.stroke();
            for (int o = 0; o < layout.optionCount(); o++) {
                AnswerSheetLayout.Point c = layout.bubbleCenter(q, o);
                centeredText(cs, bold, 5.5f, BUBBLE_LETTER, (float) c.x(), (float) c.y() + 2,
                        String.valueOf(AnswerSheetLayout.optionLetter(o)));
            }
        }
    }

    private void drawQr(PDPageContentStream cs, String content) throws IOException {
        BitMatrix matrix;
        try {
            matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.MARGIN, 0,
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalStateException("Could not encode the QR code", e);
        }
        double module = AnswerSheetLayout.QR_SIZE / matrix.getWidth();
        cs.setNonStrokingColor(0f);
        for (int y = 0; y < matrix.getHeight(); y++) {
            for (int x = 0; x < matrix.getWidth(); x++) {
                if (matrix.get(x, y)) {
                    double px = AnswerSheetLayout.QR_X + x * module;
                    double py = PAGE_H - (AnswerSheetLayout.QR_Y + (y + 1) * module);
                    cs.addRect((float) px, (float) py, (float) module + 0.05f, (float) module + 0.05f);
                }
            }
        }
        cs.fill();
    }

    // ---------------------------------------------------------------- primitivas ({@code yTop} desde arriba)

    private static void roundedRect(PDPageContentStream cs, float x, float yTop, float w, float h, float r)
            throws IOException {
        float bottom = (float) (PAGE_H - yTop - h);
        float top = (float) (PAGE_H - yTop);
        float k = 0.5523f * r;
        cs.moveTo(x + r, bottom);
        cs.lineTo(x + w - r, bottom);
        cs.curveTo(x + w - r + k, bottom, x + w, bottom + r - k, x + w, bottom + r);
        cs.lineTo(x + w, top - r);
        cs.curveTo(x + w, top - r + k, x + w - r + k, top, x + w - r, top);
        cs.lineTo(x + r, top);
        cs.curveTo(x + r - k, top, x, top - r + k, x, top - r);
        cs.lineTo(x, bottom + r);
        cs.curveTo(x, bottom + r - k, x + r - k, bottom, x + r, bottom);
        cs.closePath();
    }

    private static void line(PDPageContentStream cs, float x1, float y1Top, float x2, float y2Top) throws IOException {
        cs.moveTo(x1, (float) (PAGE_H - y1Top));
        cs.lineTo(x2, (float) (PAGE_H - y2Top));
        cs.stroke();
    }

    private static void polyline(PDPageContentStream cs, float... xyTop) throws IOException {
        cs.moveTo(xyTop[0], (float) (PAGE_H - xyTop[1]));
        for (int i = 2; i < xyTop.length; i += 2) {
            cs.lineTo(xyTop[i], (float) (PAGE_H - xyTop[i + 1]));
        }
        cs.stroke();
    }

    /** {@code yTop} es la línea base medida desde el borde superior de la página. */
    private static void text(PDPageContentStream cs, PDFont font, float size, float gray, float x, float yTop,
                             String value) throws IOException {
        PdfPrimitives.text(cs, font, size, gray, x, (float) (PAGE_H - yTop), value);
    }

    private static void centeredText(PDPageContentStream cs, PDFont font, float size, float gray, float centerX,
                                     float yTop, String value) throws IOException {
        text(cs, font, size, gray, centerX - PdfPrimitives.width(font, size, value) / 2, yTop, value);
    }
}
