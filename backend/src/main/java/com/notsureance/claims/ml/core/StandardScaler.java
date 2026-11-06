package com.notsureance.claims.ml.core;

public final class StandardScaler {

    private final double[] means;
    private final double[] scales;

    private StandardScaler(double[] means, double[] scales) {
        this.means = means;
        this.scales = scales;
    }

    public static StandardScaler fit(double[][] rows) {
        int width = rows[0].length;
        double[] means = new double[width];
        double[] scales = new double[width];
        for (double[] row : rows) {
            for (int j = 0; j < width; j++) {
                means[j] += row[j] / rows.length;
            }
        }
        for (double[] row : rows) {
            for (int j = 0; j < width; j++) {
                scales[j] += Math.pow(row[j] - means[j], 2) / rows.length;
            }
        }
        for (int j = 0; j < width; j++) {
            scales[j] = Math.sqrt(scales[j]);
            if (scales[j] < 1e-9) {
                scales[j] = 1.0;
            }
        }
        return new StandardScaler(means, scales);
    }

    public double[] transform(double[] row) {
        double[] scaled = new double[row.length];
        for (int j = 0; j < row.length; j++) {
            scaled[j] = (row[j] - means[j]) / scales[j];
        }
        return scaled;
    }

    public double[][] transformAll(double[][] rows) {
        double[][] scaled = new double[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            scaled[i] = transform(rows[i]);
        }
        return scaled;
    }
}
