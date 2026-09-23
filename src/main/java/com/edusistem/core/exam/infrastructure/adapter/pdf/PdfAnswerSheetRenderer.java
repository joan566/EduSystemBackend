package com.edusistem.core.exam.infrastructure.adapter.pdf;

import com.edusistem.core.exam.domain.outputports.AnswerSheetRendererPort;
import com.edusistem.core.exam.domain.vo.AnswerSheetData;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
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

/** Dibuja la hoja con las coordenadas exactas de {@link AnswerSheetLayout} (vectorial y determinista). */
@Component
public class PdfAnswerSheetRenderer implements AnswerSheetRendererPort {

    private static final double PAGE_H = AnswerSheetLayout.PAGE_HEIGHT;
    private static final float TEXT_LEFT = 60;
    private static final float TEXT_MAX_WIDTH = 370;

    private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Override
    public byte[] render(List<AnswerSheetData> sheets) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (AnswerSheetData sheet : sheets) {
                PDPage page = new PDPage(new PDRectangle((float) AnswerSheetLayout.PAGE_WIDTH, (float) PAGE_H));
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    drawSheet(cs, sheet);
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not generate the answer sheet PDF", e);
        }
    }

    private void drawSheet(PDPageContentStream cs, AnswerSheetData sheet) throws IOException {
        AnswerSheetLayout layout = sheet.layout();
        cs.setNonStrokingColor(0f);
        for (AnswerSheetLayout.Marker m : layout.markers()) {
            cs.addRect((float) (m.centerX() - m.size() / 2), (float) (PAGE_H - m.centerY() - m.size() / 2),
                    (float) m.size(), (float) m.size());
            cs.fill();
        }
        drawQr(cs, sheet.qrContent());
        text(cs, bold, 16, TEXT_LEFT, 82, "HOJA DE RESPUESTAS");
        text(cs, bold, 12, TEXT_LEFT, 104, fit(sheet.examName(), bold, 12));
        text(cs, regular, 10, TEXT_LEFT, 124, fit("Asignatura: " + sheet.subjectName(), regular, 10));
        text(cs, regular, 10, TEXT_LEFT, 140, fit("Grupo: " + sheet.groupName(), regular, 10));
        text(cs, regular, 10, TEXT_LEFT, 156, fit("Estudiante: " + sheet.studentName(), regular, 10));
        text(cs, regular, 10, TEXT_LEFT, 172, fit("Codigo: " + sheet.studentCode(), regular, 10));
        text(cs, regular, 7.5f, TEXT_LEFT, 196, "Rellene completamente UN solo circulo por pregunta con lapiz oscuro o boligrafo negro.");
        text(cs, regular, 7.5f, TEXT_LEFT, 206, "No escriba ni marque fuera de los circulos, ni sobre los cuadrados negros ni el codigo QR.");

        for (int column = 0; column < layout.columns(); column++) {
            for (int option = 0; option < layout.optionCount(); option++) {
                AnswerSheetLayout.Point p = layout.optionHeaderPosition(column, option);
                text(cs, bold, 9, (float) p.x(), (float) p.y(), String.valueOf(AnswerSheetLayout.optionLetter(option)));
            }
        }
        cs.setStrokingColor(0f);
        cs.setLineWidth(0.8f);
        for (int q = 1; q <= layout.numberOfQuestions(); q++) {
            AnswerSheetLayout.Point label = layout.questionLabelPosition(q);
            text(cs, regular, 8.5f, (float) label.x(), (float) label.y(), q + ".");
            for (int o = 0; o < layout.optionCount(); o++) {
                AnswerSheetLayout.Point c = layout.bubbleCenter(q, o);
                circle(cs, c.x(), PAGE_H - c.y(), AnswerSheetLayout.BUBBLE_RADIUS);
                cs.stroke();
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

    private static void circle(PDPageContentStream cs, double cx, double cy, double r) throws IOException {
        double k = 0.5523 * r;
        cs.moveTo((float) (cx + r), (float) cy);
        cs.curveTo((float) (cx + r), (float) (cy + k), (float) (cx + k), (float) (cy + r), (float) cx, (float) (cy + r));
        cs.curveTo((float) (cx - k), (float) (cy + r), (float) (cx - r), (float) (cy + k), (float) (cx - r), (float) cy);
        cs.curveTo((float) (cx - r), (float) (cy - k), (float) (cx - k), (float) (cy - r), (float) cx, (float) (cy - r));
        cs.curveTo((float) (cx + k), (float) (cy - r), (float) (cx + r), (float) (cy - k), (float) (cx + r), (float) cy);
        cs.closePath();
    }

    /** {@code yTop} es la línea base medida desde el borde superior de la página. */
    private void text(PDPageContentStream cs, PDFont font, float size, float x, float yTop, String value) throws IOException {
        cs.beginText();
        cs.setNonStrokingColor(0f);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, (float) (PAGE_H - yTop));
        cs.showText(sanitize(font, value));
        cs.endText();
    }

    private String fit(String value, PDFont font, float size) throws IOException {
        String clean = sanitize(font, value);
        while (clean.length() > 1 && font.getStringWidth(clean) / 1000 * size > TEXT_MAX_WIDTH) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    /** Sustituye por '?' los caracteres que la fuente estándar no puede codificar. */
    private static String sanitize(PDFont font, String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : (value == null ? "" : value).toCharArray()) {
            try {
                font.encode(String.valueOf(c));
                sb.append(c);
            } catch (IllegalArgumentException | IOException e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
