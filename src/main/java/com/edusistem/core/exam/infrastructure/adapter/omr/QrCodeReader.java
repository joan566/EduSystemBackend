package com.edusistem.core.exam.infrastructure.adapter.omr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Detecta y decodifica el QR de la hoja con ZXing (varios binarizadores y una versión reducida como respaldo). */
final class QrCodeReader {

    private static final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);

    static {
        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
        HINTS.put(DecodeHintType.CHARACTER_SET, "UTF-8");
    }

    Optional<String> read(BufferedImage image) {
        Optional<String> direct = decode(image);
        if (direct.isPresent()) {
            return direct;
        }
        int max = Math.max(image.getWidth(), image.getHeight());
        for (int target : new int[]{1600, 1000}) {
            if (max > target) {
                Optional<String> scaled = decode(scale(image, target / (double) max));
                if (scaled.isPresent()) {
                    return scaled;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> decode(BufferedImage image) {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        for (boolean hybrid : new boolean[]{true, false}) {
            BinaryBitmap bitmap = new BinaryBitmap(hybrid ? new HybridBinarizer(source) : new GlobalHistogramBinarizer(source));
            try {
                Result result = new MultiFormatReader().decode(bitmap, HINTS);
                return Optional.ofNullable(result.getText());
            } catch (NotFoundException e) {
                // probar con el siguiente binarizador
            }
        }
        return Optional.empty();
    }

    private static BufferedImage scale(BufferedImage source, double factor) {
        int w = Math.max(1, (int) Math.round(source.getWidth() * factor));
        int h = Math.max(1, (int) Math.round(source.getHeight() * factor));
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }
}
