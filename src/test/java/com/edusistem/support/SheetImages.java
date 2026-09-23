package com.edusistem.support;

import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/** Utilidades de test para simular la foto de una hoja: rasteriza el PDF y "rellena" burbujas. */
public final class SheetImages {

    private SheetImages() {
    }

    public static BufferedImage render(byte[] pdf, int page, float dpi) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFRenderer(document).renderImageWithDPI(page, dpi);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static int pageCount(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Rellena la burbuja (pregunta base 1, opción base 0) con un óvalo del radio y gris indicados (en puntos PDF). */
    public static void fill(BufferedImage image, AnswerSheetLayout layout, float dpi, int question, int option,
                            double radiusPt, int gray) {
        double scale = dpi / 72.0;
        AnswerSheetLayout.Point c = layout.bubbleCenter(question, option);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(gray, gray, gray));
        g.fillOval((int) Math.round((c.x() - radiusPt) * scale), (int) Math.round((c.y() - radiusPt) * scale),
                (int) Math.round(2 * radiusPt * scale), (int) Math.round(2 * radiusPt * scale));
        g.dispose();
    }

    /** Marca completa con lápiz oscuro. */
    public static void mark(BufferedImage image, AnswerSheetLayout layout, float dpi, int question, int option) {
        fill(image, layout, dpi, question, option, 5.0, 35);
    }

    public static BufferedImage rotate180(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.rotate(Math.PI, src.getWidth() / 2.0, src.getHeight() / 2.0);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Simula una foto: hoja girada, reducida y desplazada sobre un fondo, con sombra, ruido y ligero desenfoque. */
    public static BufferedImage photo(BufferedImage sheet, double degrees, double scale) {
        int w = (int) (sheet.getWidth() * 1.25);
        int h = (int) (sheet.getHeight() * 1.15);
        BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(new Color(120, 90, 60)); // mesa
        g.fillRect(0, 0, w, h);
        AffineTransform t = new AffineTransform();
        t.translate(w / 2.0, h / 2.0);
        t.rotate(Math.toRadians(degrees));
        t.scale(scale, scale);
        t.translate(-sheet.getWidth() / 2.0, -sheet.getHeight() / 2.0);
        g.drawImage(sheet, t, null);
        g.dispose();

        Random random = new Random(42);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = canvas.getRGB(x, y);
                double shade = 0.78 + 0.22 * (x / (double) w); // sombra de izquierda a derecha
                int noise = (int) (random.nextGaussian() * 4);
                int r = clamp((int) (((rgb >> 16) & 0xFF) * shade) + noise);
                int gr = clamp((int) (((rgb >> 8) & 0xFF) * shade) + noise);
                int b = clamp((int) ((rgb & 0xFF) * shade) + noise);
                canvas.setRGB(x, y, (r << 16) | (gr << 8) | b);
            }
        }
        float[] blur = {1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f, 1 / 9f};
        return new ConvolveOp(new Kernel(3, 3, blur), ConvolveOp.EDGE_NO_OP, null).filter(canvas, null);
    }

    public static byte[] png(BufferedImage image) {
        return encode(image, "png");
    }

    public static byte[] jpeg(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return encode(rgb, "jpg");
    }

    private static byte[] encode(BufferedImage image, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
