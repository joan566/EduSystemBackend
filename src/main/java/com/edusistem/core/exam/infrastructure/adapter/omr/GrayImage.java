package com.edusistem.core.exam.infrastructure.adapter.omr;

import java.awt.image.BufferedImage;

/** Imagen en escala de grises (0..255) con muestreo bilineal. Coordenadas continuas: el píxel i cubre [i, i+1). */
final class GrayImage {

    final int width;
    final int height;
    final float[] pixels;

    private GrayImage(int width, int height, float[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    /** Convierte a luminancia y reduce (promedio por bloques) para que el lado mayor no supere {@code maxSide}. */
    static GrayImage from(BufferedImage image, int maxSide) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] rgb = image.getRGB(0, 0, w, h, null, 0, w);
        double scale = Math.max(1.0, Math.max(w, h) / (double) maxSide);
        int dw = Math.max(1, (int) Math.round(w / scale));
        int dh = Math.max(1, (int) Math.round(h / scale));
        float[] out = new float[dw * dh];
        for (int dy = 0; dy < dh; dy++) {
            int sy0 = (int) Math.floor(dy * scale);
            int sy1 = Math.max(sy0 + 1, Math.min(h, (int) Math.ceil((dy + 1) * scale)));
            for (int dx = 0; dx < dw; dx++) {
                int sx0 = (int) Math.floor(dx * scale);
                int sx1 = Math.max(sx0 + 1, Math.min(w, (int) Math.ceil((dx + 1) * scale)));
                double sum = 0;
                int count = 0;
                for (int y = sy0; y < sy1 && y < h; y++) {
                    for (int x = sx0; x < sx1 && x < w; x++) {
                        int p = rgb[y * w + x];
                        sum += 0.299 * ((p >> 16) & 0xFF) + 0.587 * ((p >> 8) & 0xFF) + 0.114 * (p & 0xFF);
                        count++;
                    }
                }
                out[dy * dw + dx] = (float) (sum / Math.max(1, count));
            }
        }
        return new GrayImage(dw, dh, out);
    }

    float at(int x, int y) {
        return pixels[y * width + x];
    }

    /** Muestreo bilineal; {@code NaN} si el punto cae fuera de la imagen. */
    double sample(double x, double y) {
        double fx = x - 0.5;
        double fy = y - 0.5;
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        if (x0 < 0 || y0 < 0 || x0 + 1 >= width || y0 + 1 >= height) {
            return Double.NaN;
        }
        double tx = fx - x0;
        double ty = fy - y0;
        double top = at(x0, y0) * (1 - tx) + at(x0 + 1, y0) * tx;
        double bottom = at(x0, y0 + 1) * (1 - tx) + at(x0 + 1, y0 + 1) * tx;
        return top * (1 - ty) + bottom * ty;
    }
}
