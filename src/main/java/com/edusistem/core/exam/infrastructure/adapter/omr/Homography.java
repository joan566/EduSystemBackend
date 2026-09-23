package com.edusistem.core.exam.infrastructure.adapter.omr;

/** Transformación proyectiva plano de la hoja (puntos PDF) → píxeles de la imagen, calculada con 4 correspondencias. */
final class Homography {

    private final double[] h;

    private Homography(double[] h) {
        this.h = h;
    }

    /** @param from 4 puntos {x, y} del layout; @param to 4 puntos {u, v} de la imagen. */
    static Homography fromPoints(double[][] from, double[][] to) {
        double[][] a = new double[8][9];
        for (int i = 0; i < 4; i++) {
            double x = from[i][0];
            double y = from[i][1];
            double u = to[i][0];
            double v = to[i][1];
            a[2 * i] = new double[]{x, y, 1, 0, 0, 0, -x * u, -y * u, u};
            a[2 * i + 1] = new double[]{0, 0, 0, x, y, 1, -x * v, -y * v, v};
        }
        double[] solution = solve(a);
        return new Homography(new double[]{solution[0], solution[1], solution[2], solution[3], solution[4],
                solution[5], solution[6], solution[7], 1});
    }

    double[] map(double x, double y) {
        double w = h[6] * x + h[7] * y + h[8];
        return new double[]{(h[0] * x + h[1] * y + h[2]) / w, (h[3] * x + h[4] * y + h[5]) / w};
    }

    /** Eliminación gaussiana con pivoteo parcial sobre la matriz aumentada 8x9. */
    private static double[] solve(double[][] a) {
        int n = 8;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(a[r][col]) > Math.abs(a[pivot][col])) {
                    pivot = r;
                }
            }
            double[] tmp = a[col];
            a[col] = a[pivot];
            a[pivot] = tmp;
            if (Math.abs(a[col][col]) < 1e-12) {
                throw new IllegalArgumentException("Degenerate point configuration");
            }
            for (int r = 0; r < n; r++) {
                if (r == col) {
                    continue;
                }
                double factor = a[r][col] / a[col][col];
                for (int c = col; c <= n; c++) {
                    a[r][c] -= factor * a[col][c];
                }
            }
        }
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = a[i][n] / a[i][i];
        }
        return x;
    }
}
