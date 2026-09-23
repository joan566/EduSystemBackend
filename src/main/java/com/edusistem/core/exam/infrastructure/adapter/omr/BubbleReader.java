package com.edusistem.core.exam.infrastructure.adapter.omr;

import com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import java.util.Arrays;

/**
 * Mide el relleno de cada burbuja en las coordenadas exactas del layout. Para cada burbuja se estima el nivel del papel
 * en un anillo vacío alrededor y se cuenta la fracción de puntos del disco interior que son claramente más oscuros
 * (robusto a sombras y a cambios de iluminación entre zonas de la hoja).
 */
final class BubbleReader {

    private static final double INNER_RADIUS_FACTOR = 0.70;
    private static final double DARK_FACTOR = 0.62;
    private static final double MIN_PAPER_LEVEL = 70;

    double[][] read(GrayImage image, Homography homography, AnswerSheetLayout layout) {
        double[][] fills = new double[layout.numberOfQuestions()][layout.optionCount()];
        double paperSum = 0;
        int paperCount = 0;
        for (int q = 1; q <= layout.numberOfQuestions(); q++) {
            for (int o = 0; o < layout.optionCount(); o++) {
                AnswerSheetLayout.Point center = layout.bubbleCenter(q, o);
                double paper = paperLevel(image, homography, center);
                paperSum += paper;
                paperCount++;
                fills[q - 1][o] = darkFraction(image, homography, center, paper);
            }
        }
        if (paperSum / paperCount < MIN_PAPER_LEVEL) {
            throw new AnswerSheetProcessingException("IMAGE_TOO_DARK", "The image is too dark to read the answer sheet");
        }
        return fills;
    }

    private double paperLevel(GrayImage image, Homography homography, AnswerSheetLayout.Point c) {
        double r = AnswerSheetLayout.BUBBLE_RADIUS;
        double[] samples = new double[3 * 16];
        int n = 0;
        for (double radius : new double[]{1.4 * r, 1.55 * r, 1.7 * r}) {
            for (int a = 0; a < 16; a++) {
                double angle = 2 * Math.PI * a / 16;
                samples[n++] = sampleAt(image, homography, c.x() + radius * Math.cos(angle), c.y() + radius * Math.sin(angle));
            }
        }
        Arrays.sort(samples);
        return samples[samples.length / 2];
    }

    private double darkFraction(GrayImage image, Homography homography, AnswerSheetLayout.Point c, double paper) {
        double radius = AnswerSheetLayout.BUBBLE_RADIUS * INNER_RADIUS_FACTOR;
        double threshold = Math.max(paper, MIN_PAPER_LEVEL) * DARK_FACTOR;
        int total = 0;
        int dark = 0;
        for (double dy = -radius; dy <= radius; dy += 0.5) {
            for (double dx = -radius; dx <= radius; dx += 0.5) {
                if (dx * dx + dy * dy > radius * radius) {
                    continue;
                }
                total++;
                if (sampleAt(image, homography, c.x() + dx, c.y() + dy) < threshold) {
                    dark++;
                }
            }
        }
        return total == 0 ? 0 : dark / (double) total;
    }

    private double sampleAt(GrayImage image, Homography homography, double x, double y) {
        double[] p = homography.map(x, y);
        double value = image.sample(p[0], p[1]);
        if (Double.isNaN(value)) {
            throw new AnswerSheetProcessingException("SHEET_OUT_OF_FRAME",
                    "Part of the answer sheet is outside the photo; retake it showing the whole sheet");
        }
        return value;
    }
}
