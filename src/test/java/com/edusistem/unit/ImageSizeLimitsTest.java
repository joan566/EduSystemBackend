package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.infrastructure.adapter.omr.AnswerSheetProcessorAdapter;
import com.edusistem.core.exam.infrastructure.adapter.pdf.PdfBoxScannedPdfAdapter;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

/** Archivos pequeños que declaran dimensiones enormes no deben agotar la memoria al decodificarse. */
class ImageSizeLimitsTest {

    @Test
    void rejectsImagesThatDeclareTooManyPixelsWithoutDecodingThem() {
        byte[] bomb = pngHeader(20_000, 20_000); // 400 MP: 1,6 GB decodificada
        AnswerSheetProcessorAdapter processor = new AnswerSheetProcessorAdapter();
        assertThatThrownBy(() -> processor.readQrCode(bomb)).isInstanceOf(InvalidRequestException.class)
                .hasFieldOrPropertyWithValue("code", "IMAGE_TOO_LARGE");
        assertThatThrownBy(() -> processor.readBubbles(bomb, new AnswerSheetLayout(10, 4)))
                .isInstanceOf(AnswerSheetProcessingException.class);
    }

    @Test
    void rasterizesHugePdfPagesAtALowerResolution() throws IOException {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(new PDRectangle(72 * 200, 72 * 200))); // 200 x 200 pulgadas
            document.save(out);
            pdf = out.toByteArray();
        }
        List<BufferedImage> pages = new ArrayList<>();
        new PdfBoxScannedPdfAdapter().forEachPage(pdf, 1, (number, jpeg) -> pages.add(read(jpeg)));
        assertThat(pages).hasSize(1);
        assertThat((long) pages.get(0).getWidth() * pages.get(0).getHeight()).isLessThanOrEqualTo(25_000_000L);
    }

    private static BufferedImage read(byte[] jpeg) {
        try {
            return ImageIO.read(new ByteArrayInputStream(jpeg));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** PNG con solo la cabecera IHDR (dimensiones declaradas) y IEND, sin datos de píxeles. */
    private static byte[] pngHeader(int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        chunk(out, "IHDR", ByteBuffer.allocate(13).putInt(width).putInt(height).put((byte) 8).put((byte) 2)
                .put((byte) 0).put((byte) 0).put((byte) 0).array());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(typeBytes);
        out.writeBytes(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
