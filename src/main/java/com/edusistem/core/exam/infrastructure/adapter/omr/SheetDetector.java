package com.edusistem.core.exam.infrastructure.adapter.omr;

import com.edusistem.core.exam.domain.exceptions.AnswerSheetProcessingException;
import com.edusistem.core.exam.domain.vo.AnswerSheetLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Localiza la hoja: binariza con umbral adaptativo, etiqueta componentes conexos, conserva los cuadrados sólidos y
 * elige el cuadrilátero de mayor área cuyos tamaños y proporciones coinciden con los 4 marcadores del layout
 * (el de arriba-izquierda es el más grande y fija la orientación, por lo que admite fotos giradas).
 */
final class SheetDetector {

    private static final int MAX_CANDIDATES = 24;

    record Blob(double cx, double cy, double side, int area) {
    }

    Homography detect(GrayImage image, AnswerSheetLayout layout) {
        List<Blob> candidates = findSolidSquares(image);
        Blob[] ordered = pickMarkers(candidates);
        if (ordered == null) {
            throw new AnswerSheetProcessingException("SHEET_NOT_DETECTED",
                    "The four corner markers of the answer sheet were not found; retake the photo showing the whole sheet");
        }
        double[][] from = new double[4][];
        double[][] to = new double[4][];
        List<AnswerSheetLayout.Marker> markers = layout.markers(); // TL, TR, BR, BL
        for (int i = 0; i < 4; i++) {
            from[i] = new double[]{markers.get(i).centerX(), markers.get(i).centerY()};
            to[i] = new double[]{ordered[i].cx(), ordered[i].cy()};
        }
        try {
            return Homography.fromPoints(from, to);
        } catch (IllegalArgumentException e) {
            throw new AnswerSheetProcessingException("SHEET_NOT_DETECTED", "The sheet geometry could not be computed");
        }
    }

    // ---------------------------------------------------------------- candidatos

    private List<Blob> findSolidSquares(GrayImage image) {
        int w = image.width;
        int h = image.height;
        boolean[] dark = binarize(image);
        int maxDim = Math.max(w, h);
        int minSide = Math.max(10, (int) (0.006 * maxDim));
        int maxSide = (int) (0.25 * maxDim);

        boolean[] visited = new boolean[w * h];
        int[] stack = new int[w * h];
        List<Blob> blobs = new ArrayList<>();
        for (int start = 0; start < dark.length; start++) {
            if (!dark[start] || visited[start]) {
                continue;
            }
            int sp = 0;
            stack[sp++] = start;
            visited[start] = true;
            int minX = w, maxX = 0, minY = h, maxY = 0, area = 0;
            double sumX = 0, sumY = 0;
            List<int[]> boundary = new ArrayList<>();
            while (sp > 0) {
                int p = stack[--sp];
                int x = p % w;
                int y = p / w;
                area++;
                sumX += x + 0.5;
                sumY += y + 0.5;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                boolean edge = false;
                for (int d = 0; d < 4; d++) {
                    int nx = x + (d == 0 ? 1 : d == 1 ? -1 : 0);
                    int ny = y + (d == 2 ? 1 : d == 3 ? -1 : 0);
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h || !dark[ny * w + nx]) {
                        edge = true;
                        continue;
                    }
                    int np = ny * w + nx;
                    if (!visited[np]) {
                        visited[np] = true;
                        stack[sp++] = np;
                    }
                }
                if (edge && area < 200_000) {
                    boundary.add(new int[]{x, y});
                }
            }
            int bw = maxX - minX + 1;
            int bh = maxY - minY + 1;
            double aspect = bw / (double) bh;
            if (bw < minSide || bh < minSide || bw > maxSide || bh > maxSide || aspect < 0.6 || aspect > 1.67
                    || area < 0.45 * bw * bh) {
                continue;
            }
            if (area / convexHullArea(boundary) < 0.93) {
                continue; // no es un cuadrado sólido (anillos, glifos, ruido)
            }
            blobs.add(new Blob(sumX / area, sumY / area, Math.sqrt(area), area));
        }
        blobs.sort(Comparator.comparingInt(Blob::area).reversed());
        return blobs.size() > MAX_CANDIDATES ? new ArrayList<>(blobs.subList(0, MAX_CANDIDATES)) : blobs;
    }

    /** Umbral adaptativo con imagen integral: oscuro si es claramente más oscuro que su vecindario. */
    private boolean[] binarize(GrayImage image) {
        int w = image.width;
        int h = image.height;
        long[] integral = new long[(w + 1) * (h + 1)];
        for (int y = 0; y < h; y++) {
            long row = 0;
            for (int x = 0; x < w; x++) {
                row += (long) image.at(x, y);
                integral[(y + 1) * (w + 1) + x + 1] = integral[y * (w + 1) + x + 1] + row;
            }
        }
        int half = Math.max(15, Math.max(w, h) / 20);
        boolean[] dark = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            int y0 = Math.max(0, y - half);
            int y1 = Math.min(h, y + half + 1);
            for (int x = 0; x < w; x++) {
                int x0 = Math.max(0, x - half);
                int x1 = Math.min(w, x + half + 1);
                long sum = integral[y1 * (w + 1) + x1] - integral[y0 * (w + 1) + x1]
                        - integral[y1 * (w + 1) + x0] + integral[y0 * (w + 1) + x0];
                double mean = sum / (double) ((x1 - x0) * (y1 - y0));
                float v = image.at(x, y);
                dark[y * w + x] = v < mean * 0.62 && v < 140;
            }
        }
        return dark;
    }

    private static double convexHullArea(List<int[]> points) {
        if (points.size() < 3) {
            return 1;
        }
        int[][] pts = points.toArray(new int[0][]);
        Arrays.sort(pts, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        int n = pts.length;
        int[][] hull = new int[2 * n][];
        int k = 0;
        for (int i = 0; i < n; i++) {
            while (k >= 2 && cross(hull[k - 2], hull[k - 1], pts[i]) <= 0) {
                k--;
            }
            hull[k++] = pts[i];
        }
        for (int i = n - 2, t = k + 1; i >= 0; i--) {
            while (k >= t && cross(hull[k - 2], hull[k - 1], pts[i]) <= 0) {
                k--;
            }
            hull[k++] = pts[i];
        }
        double area = 0;
        for (int i = 0; i < k - 1; i++) {
            area += (double) hull[i][0] * hull[i + 1][1] - (double) hull[i + 1][0] * hull[i][1];
        }
        return Math.max(1, Math.abs(area) / 2 + points.size() * 0.25);
    }

    private static long cross(int[] o, int[] a, int[] b) {
        return (long) (a[0] - o[0]) * (b[1] - o[1]) - (long) (a[1] - o[1]) * (b[0] - o[0]);
    }

    // ---------------------------------------------------------------- selección de los 4 marcadores

    /** Devuelve los marcadores en el orden del layout (TL, TR, BR, BL) o null. */
    private Blob[] pickMarkers(List<Blob> blobs) {
        int n = blobs.size();
        Blob[] best = null;
        double bestArea = 0;
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                for (int c = b + 1; c < n; c++) {
                    for (int d = c + 1; d < n; d++) {
                        Blob[] ordered = arrange(new Blob[]{blobs.get(a), blobs.get(b), blobs.get(c), blobs.get(d)});
                        if (ordered == null) {
                            continue;
                        }
                        double quadArea = quadArea(ordered);
                        if (quadArea > bestArea) {
                            bestArea = quadArea;
                            best = ordered;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Ordena en sentido horario empezando por el marcador grande (TL) y valida tamaños y proporciones. */
    private Blob[] arrange(Blob[] four) {
        double cx = Arrays.stream(four).mapToDouble(Blob::cx).average().orElse(0);
        double cy = Arrays.stream(four).mapToDouble(Blob::cy).average().orElse(0);
        Blob[] byAngle = four.clone();
        Arrays.sort(byAngle, Comparator.comparingDouble(p -> Math.atan2(p.cy() - cy, p.cx() - cx)));
        if (!isConvex(byAngle)) {
            return null;
        }
        int big = 0;
        for (int i = 1; i < 4; i++) {
            if (byAngle[i].side() > byAngle[big].side()) {
                big = i;
            }
        }
        Blob[] ordered = new Blob[4];
        for (int i = 0; i < 4; i++) {
            ordered[i] = byAngle[(big + i) % 4];
        }
        double[] others = {ordered[1].side(), ordered[2].side(), ordered[3].side()};
        Arrays.sort(others);
        double ratio = ordered[0].side() / others[1];
        if (ratio < 1.3 || ratio > 2.4 || others[2] / others[0] > 1.5) {
            return null;
        }
        double width = dist(ordered[0], ordered[1]);
        double height = dist(ordered[0], ordered[3]);
        double width2 = dist(ordered[3], ordered[2]);
        double height2 = dist(ordered[1], ordered[2]);
        if (!plausible(width / height) || !plausible(width2 / height2)) {
            return null;
        }
        return ordered;
    }

    private static boolean plausible(double aspect) {
        return aspect > 0.45 && aspect < 0.95; // ancho/alto esperado ≈ 0.68 (con tolerancia de perspectiva)
    }

    private static boolean isConvex(Blob[] p) {
        boolean positive = false;
        boolean negative = false;
        for (int i = 0; i < 4; i++) {
            Blob a = p[i];
            Blob b = p[(i + 1) % 4];
            Blob c = p[(i + 2) % 4];
            double cross = (b.cx() - a.cx()) * (c.cy() - b.cy()) - (b.cy() - a.cy()) * (c.cx() - b.cx());
            positive |= cross > 0;
            negative |= cross < 0;
        }
        return !(positive && negative);
    }

    private static double dist(Blob a, Blob b) {
        return Math.hypot(a.cx() - b.cx(), a.cy() - b.cy());
    }

    private static double quadArea(Blob[] p) {
        double area = 0;
        for (int i = 0; i < 4; i++) {
            Blob a = p[i];
            Blob b = p[(i + 1) % 4];
            area += a.cx() * b.cy() - b.cx() * a.cy();
        }
        return Math.abs(area) / 2;
    }
}
