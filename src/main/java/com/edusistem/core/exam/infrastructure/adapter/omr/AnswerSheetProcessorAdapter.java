package com.edusistem.core.exam.infrastructure.adapter.omr;

import com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException;
import com.edusistem.core.exam.domain.outputports.AnswerSheetProcessorPort;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import com.edusistem.core.exam.domain.vo.BubbleReading;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;

/**
 * Implementación Java pura (ZXing + procesamiento propio): detección de QR, detección de la hoja por sus marcadores de
 * esquina y lectura de burbujas. Puede sustituirse por otra (p. ej. OpenCV) sin tocar dominio ni aplicación.
 */
@Component
public class AnswerSheetProcessorAdapter implements AnswerSheetProcessorPort {

    private static final int WORKING_SIZE = 2000;
    /**
     * Máximo de píxeles que se aceptan decodificar (~50 MP, más que una foto de móvil a resolución completa). Una
     * imagen pequeña en bytes puede declarar dimensiones enormes y agotar la memoria al decodificarla.
     */
    static final long MAX_PIXELS = 50_000_000L;

    private final QrCodeReader qrReader = new QrCodeReader();
    private final SheetDetector sheetDetector = new SheetDetector();
    private final BubbleReader bubbleReader = new BubbleReader();

    @Override
    public Optional<String> readQrCode(byte[] image) {
        if (tooLarge(image)) {
            throw new InvalidRequestException("IMAGE_TOO_LARGE",
                    "The image resolution is too high; the maximum is " + MAX_PIXELS / 1_000_000 + " megapixels");
        }
        BufferedImage decoded = decode(image);
        if (decoded == null) {
            throw new InvalidRequestException("INVALID_IMAGE", "The file could not be decoded as an image");
        }
        return qrReader.read(decoded);
    }

    @Override
    public BubbleReading readBubbles(byte[] image, AnswerSheetLayout layout) {
        if (tooLarge(image)) {
            throw new AnswerSheetProcessingException("IMAGE_TOO_LARGE", "The image resolution is too high");
        }
        BufferedImage decoded = decode(image);
        if (decoded == null) {
            throw new AnswerSheetProcessingException("IMAGE_UNREADABLE", "The file could not be decoded as an image");
        }
        GrayImage gray = GrayImage.from(decoded, WORKING_SIZE);
        Homography homography = sheetDetector.detect(gray, layout);
        return new BubbleReading(bubbleReader.read(gray, homography, layout));
    }

    /** Lee solo la cabecera (sin decodificar los píxeles) para conocer las dimensiones. */
    static boolean tooLarge(byte[] image) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                return false; // no es una imagen conocida: decode() lo rechazará
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                return (long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static BufferedImage decode(byte[] image) {
        try {
            return ImageIO.read(new ByteArrayInputStream(image));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
