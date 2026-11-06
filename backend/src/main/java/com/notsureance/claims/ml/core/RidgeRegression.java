package com.notsureance.claims.ml.core;

public final class RidgeRegression {

    private final double[] weights;
    private final double intercept;

    private RidgeRegression(double[] weights, double intercept) {
        this.weights = weights;
        this.intercept = intercept;
    }

    public static RidgeRegression fit(double[][] x, double[] y, double lambda) {
        int width = x[0].length;
        double meanY = 0;
        double[] meanX = new double[width];
        for (int i = 0; i < x.length; i++) {
            meanY += y[i] / y.length;
            for (int a = 0; a < width; a++) {
                meanX[a] += x[i][a] / x.length;
            }
        }
        double[][] gram = new double[width][width];
        double[] moment = new double[width];
        for (int i = 0; i < x.length; i++) {
            for (int a = 0; a < width; a++) {
                double centeredA = x[i][a] - meanX[a];
                moment[a] += centeredA * (y[i] - meanY);
                for (int b = 0; b < width; b++) {
                    gram[a][b] += centeredA * (x[i][b] - meanX[b]);
                }
            }
        }
        for (int a = 0; a < width; a++) {
            gram[a][a] += lambda;
        }
        double[] weights = solve(gram, moment);
        return new RidgeRegression(weights, meanY - LogisticRegression.dot(weights, meanX));
    }

    public double predict(double[] x) {
        return intercept + LogisticRegression.dot(weights, x);
    }

    static double[] solve(double[][] matrix, double[] rhs) {
        int n = rhs.length;
        double[][] a = new double[n][];
        double[] b = rhs.clone();
        for (int i = 0; i < n; i++) {
            a[i] = matrix[i].clone();
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(a[row][col]) > Math.abs(a[pivot][col])) {
                    pivot = row;
                }
            }
            double[] swapRow = a[col];
            a[col] = a[pivot];
            a[pivot] = swapRow;
            double swapValue = b[col];
            b[col] = b[pivot];
            b[pivot] = swapValue;
            for (int row = col + 1; row < n; row++) {
                double factor = a[row][col] / a[col][col];
                b[row] -= factor * b[col];
                for (int k = col; k < n; k++) {
                    a[row][k] -= factor * a[col][k];
                }
            }
        }
        double[] solution = new double[n];
        for (int row = n - 1; row >= 0; row--) {
            double sum = b[row];
            for (int k = row + 1; k < n; k++) {
                sum -= a[row][k] * solution[k];
            }
            solution[row] = sum / a[row][row];
        }
        return solution;
    }
}
