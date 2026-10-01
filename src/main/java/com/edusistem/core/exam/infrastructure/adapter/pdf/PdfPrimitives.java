package com.edusistem.core.exam.infrastructure.adapter.pdf;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

/** Dibujo y medición de texto y figuras que comparten los documentos PDF del examen (coordenadas PDF: y desde abajo). */
final class PdfPrimitives {

    /** Aproximación de un cuarto de círculo con una curva de Bézier. */
    private static final double KAPPA = 0.5523;

    private PdfPrimitives() {
    }

    static void circle(PDPageContentStream cs, double cx, double cy, double r) throws IOException {
        double k = KAPPA * r;
        cs.moveTo((float) (cx + r), (float) cy);
        cs.curveTo((float) (cx + r), (float) (cy + k), (float) (cx + k), (float) (cy + r), (float) cx, (float) (cy + r));
        cs.curveTo((float) (cx - k), (float) (cy + r), (float) (cx - r), (float) (cy + k), (float) (cx - r), (float) cy);
        cs.curveTo((float) (cx - r), (float) (cy - k), (float) (cx - k), (float) (cy - r), (float) cx, (float) (cy - r));
        cs.curveTo((float) (cx + k), (float) (cy - r), (float) (cx + r), (float) (cy - k), (float) (cx + r), (float) cy);
        cs.closePath();
    }

    /** Texto con la línea base en ({@code x}, {@code y}). */
    static void text(PDPageContentStream cs, PDFont font, float size, float gray, float x, float y, String value)
            throws IOException {
        cs.beginText();
        cs.setNonStrokingColor(gray);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(font, value));
        cs.endText();
    }

    static float width(PDFont font, float size, String value) throws IOException {
        return font.getStringWidth(sanitize(font, value)) / 1000 * size;
    }

    /** Recorta el texto al ancho disponible añadiendo "…" si no cabe. */
    static String fit(String value, PDFont font, float size, float maxWidth) throws IOException {
        String clean = sanitize(font, value).strip();
        if (width(font, size, clean) <= maxWidth) {
            return clean;
        }
        while (clean.length() > 1 && width(font, size, clean.strip() + "…") > maxWidth) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean.strip() + "…";
    }

    /** Sustituye por '?' los caracteres que la fuente no puede codificar. */
    static String sanitize(PDFont font, String value) {
        StringBuilder sb = new StringBuilder();
        (value == null ? "" : value).codePoints().forEach(cp -> {
            String c = Character.toString(cp);
            try {
                font.encode(c);
                sb.append(c);
            } catch (IllegalArgumentException | IOException e) {
                sb.append('?');
            }
        });
        return sb.toString();
    }
}
