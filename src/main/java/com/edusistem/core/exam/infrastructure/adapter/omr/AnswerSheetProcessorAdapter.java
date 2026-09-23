package com.edusistem.core.exam.infrastructure.adapter.omr;

import com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException;
import com.edusistem.core.exam.domain.outputports.AnswerSheetProcessorPort;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.BubbleReading;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Implementación Java pura (ZXing + procesamiento propio): detección de QR, detección de la hoja por sus marcadores de
 * esquina y lectura de burbujas. Puede sustituirse por otra (p. ej. OpenCV) sin tocar dominio ni aplicación.
 */
@Component
public class AnswerSheetProcessorAdapter implements AnswerSheetProcessorPort {

    private static final int WORKING_SIZE = 2000;

    private final QrCodeReader qrReader = new QrCodeReader();
    private final SheetDetector sheetDetector = new SheetDetector();
    private final BubbleReader bubbleReader = new BubbleReader();

    @Override
    public Optional<String> readQrCode(byte[] image) {
        BufferedImage decoded = decode(image);
        if (decoded == null) {
            throw new InvalidRequestException("INVALID_IMAGE", "The file could not be decoded as an image");
        }
        return qrReader.read(decoded);
    }

    @Override
    public BubbleReading readBubbles(byte[] image, AnswerSheetLayout layout) {
        BufferedImage decoded = decode(image);
        if (decoded == null) {
            throw new AnswerSheetProcessingException("IMAGE_UNREADABLE", "The file could not be decoded as an image");
        }
        GrayImage gray = GrayImage.from(decoded, WORKING_SIZE);
        Homography homography = sheetDetector.detect(gray, layout);
        return new BubbleReading(bubbleReader.read(gray, homography, layout));
    }

    private static BufferedImage decode(byte[] image) {
        try {
            return ImageIO.read(new ByteArrayInputStream(image));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
