package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.exam.domain.enums.AnswerDetectionStatus;
import com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException;
import com.edusistem.core.exam.domain.service.BubbleClassifier;
import com.edusistem.core.exam.domain.vo.AnswerSheetData;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.BubbleReading;
import com.edusistem.core.exam.domain.vo.DetectedAnswer;
import com.edusistem.core.exam.domain.vo.OmrThresholds;
import com.edusistem.core.exam.domain.vo.QrPayload;
import com.edusistem.core.exam.infrastructure.adapter.omr.AnswerSheetProcessorAdapter;
import com.edusistem.core.exam.infrastructure.adapter.pdf.PdfAnswerSheetRenderer;
import com.edusistem.support.SheetImages;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Recorre el pipeline real: PDF generado → "foto" con marcas → QR + hoja + burbujas → clasificación. */
class AnswerSheetProcessingTest {

    private static final float DPI = 200;
    private static final AnswerSheetLayout LAYOUT = new AnswerSheetLayout(30, 4);
    private static final String QR = QrPayload.encode(7, "EST-000123");

    private static final AnswerSheetProcessorAdapter PROCESSOR = new AnswerSheetProcessorAdapter();
    private static final BubbleClassifier CLASSIFIER = new BubbleClassifier(OmrThresholds.defaults());
    private static byte[] pdf;

    @BeforeAll
    static void generateSheet() {
        pdf = new PdfAnswerSheetRenderer().render(List.of(new AnswerSheetData("Parcial 1 - Álgebra", "Matemáticas",
                "10° A", "Pérez Ana", "EST-000123", QR, LAYOUT)));
    }

    private static BufferedImage blankSheet() {
        return SheetImages.render(pdf, 0, DPI);
    }

    /** Respuestas conocidas: 1→A 2→B 3→C 4→D 5→(vacía) 6→A+C (múltiple) 7→B con marca débil; resto→A. */
    private static BufferedImage answeredSheet() {
        BufferedImage img = blankSheet();
        SheetImages.mark(img, LAYOUT, DPI, 1, 0);
        SheetImages.mark(img, LAYOUT, DPI, 2, 1);
        SheetImages.mark(img, LAYOUT, DPI, 3, 2);
        SheetImages.mark(img, LAYOUT, DPI, 4, 3);
        SheetImages.mark(img, LAYOUT, DPI, 6, 0);
        SheetImages.mark(img, LAYOUT, DPI, 6, 2);
        SheetImages.fill(img, LAYOUT, DPI, 7, 1, 2.6, 60); // marca pequeña/débil
        for (int q = 8; q <= 30; q++) {
            SheetImages.mark(img, LAYOUT, DPI, q, 0);
        }
        return img;
    }

    private static void assertReadsExpectedAnswers(byte[] image) {
        assertThat(PROCESSOR.readQrCode(image)).contains(QR);
        BubbleReading reading = PROCESSOR.readBubbles(image, LAYOUT);
        List<DetectedAnswer> answers = CLASSIFIER.classify(reading);
        assertThat(answers).hasSize(30);
        assertThat(answers.get(0)).matches(a -> a.status() == AnswerDetectionStatus.MARKED && "A".equals(a.selectedOption()));
        assertThat(answers.get(1)).matches(a -> a.status() == AnswerDetectionStatus.MARKED && "B".equals(a.selectedOption()));
        assertThat(answers.get(2)).matches(a -> a.status() == AnswerDetectionStatus.MARKED && "C".equals(a.selectedOption()));
        assertThat(answers.get(3)).matches(a -> a.status() == AnswerDetectionStatus.MARKED && "D".equals(a.selectedOption()));
        assertThat(answers.get(4).status()).isEqualTo(AnswerDetectionStatus.EMPTY);
        assertThat(answers.get(4).selectedOption()).isNull();
        assertThat(answers.get(5).status()).isEqualTo(AnswerDetectionStatus.MULTIPLE_MARK);
        assertThat(answers.get(5).selectedOption()).isNull();
        assertThat(answers.get(6).status()).isEqualTo(AnswerDetectionStatus.REVIEW_REQUIRED);
        for (int q = 8; q <= 30; q++) {
            assertThat(answers.get(q - 1).selectedOption()).as("question %d", q).isEqualTo("A");
            assertThat(answers.get(q - 1).status()).as("question %d", q).isEqualTo(AnswerDetectionStatus.MARKED);
        }
    }

    @Test
    void generatedPdfHasOnePagePerSheet() {
        assertThat(SheetImages.pageCount(pdf)).isEqualTo(1);
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void readsQrAndAnswersFromACleanScan() {
        assertReadsExpectedAnswers(SheetImages.png(answeredSheet()));
    }

    @Test
    void readsAnswersFromAnUpsideDownPhotoWithShadowNoiseAndBlur() {
        BufferedImage photo = SheetImages.photo(SheetImages.rotate180(answeredSheet()), 0, 0.8);
        assertReadsExpectedAnswers(SheetImages.jpeg(photo));
    }

    @Test
    void readsAnswersFromASlightlyRotatedPhoto() {
        BufferedImage photo = SheetImages.photo(answeredSheet(), 7, 0.75);
        assertReadsExpectedAnswers(SheetImages.jpeg(photo));
    }

    @Test
    void blankSheetIsReadAsAllEmpty() {
        byte[] image = SheetImages.png(blankSheet());
        List<DetectedAnswer> answers = CLASSIFIER.classify(PROCESSOR.readBubbles(image, LAYOUT));
        assertThat(answers).allMatch(a -> a.status() == AnswerDetectionStatus.EMPTY && a.selectedOption() == null);
    }

    @Test
    void imageWithoutQrYieldsEmptyOptional() {
        BufferedImage img = new BufferedImage(600, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 600, 800);
        g.dispose();
        Optional<String> qr = PROCESSOR.readQrCode(SheetImages.png(img));
        assertThat(qr).isEmpty();
    }

    @Test
    void imageThatIsNotASheetFailsWithSheetNotDetected() {
        BufferedImage img = new BufferedImage(800, 1100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 800, 1100);
        g.dispose();
        assertThatThrownBy(() -> PROCESSOR.readBubbles(SheetImages.png(img), LAYOUT))
                .isInstanceOf(AnswerSheetProcessingException.class)
                .extracting("code").isEqualTo("SHEET_NOT_DETECTED");
    }

    @Test
    void supportsSheetsWithSeveralColumnsAndMoreOptions() {
        AnswerSheetLayout big = new AnswerSheetLayout(60, 5);
        byte[] bigPdf = new PdfAnswerSheetRenderer().render(List.of(new AnswerSheetData("Examen largo", "Física", "11° B",
                "Gómez Luis", "EST-000999", QrPayload.encode(9, "EST-000999"), big)));
        BufferedImage img = SheetImages.render(bigPdf, 0, DPI);
        SheetImages.mark(img, big, DPI, 1, 4);   // columna 1, opción E
        SheetImages.mark(img, big, DPI, 30, 2);  // columna 2
        SheetImages.mark(img, big, DPI, 60, 1);  // última pregunta, columna 3
        List<DetectedAnswer> answers = CLASSIFIER.classify(PROCESSOR.readBubbles(SheetImages.png(img), big));
        assertThat(answers.get(0).selectedOption()).isEqualTo("E");
        assertThat(answers.get(29).selectedOption()).isEqualTo("C");
        assertThat(answers.get(59).selectedOption()).isEqualTo("B");
        assertThat(answers.get(1).status()).isEqualTo(AnswerDetectionStatus.EMPTY);
    }
}
