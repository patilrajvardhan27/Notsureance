package com.notsureance.claims.ml.core;

public final class LogisticRegression {

    private final double[] weights;
    private final double bias;

    private LogisticRegression(double[] weights, double bias) {
        this.weights = weights;
        this.bias = bias;
    }

    public record Options(int epochs, double learningRate, double l2Penalty, boolean balanceClasses) {

        public static Options defaults() {
            return new Options(600, 0.3, 0.01, true);
        }
    }

    public static LogisticRegression fit(double[][] x, int[] y, Options options) {
        int rows = x.length;
        int width = x[0].length;
        double positives = 0;
        for (int label : y) {
            positives += label;
        }
        double positiveWeight = options.balanceClasses() && positives > 0 ? (rows - positives) / positives : 1.0;
        double totalWeight = positives * positiveWeight + (rows - positives);

        double[] weights = new double[width];
        double bias = 0;
        for (int epoch = 0; epoch < options.epochs(); epoch++) {
            double[] gradient = new double[width];
            double biasGradient = 0;
            for (int i = 0; i < rows; i++) {
                double sampleWeight = y[i] == 1 ? positiveWeight : 1.0;
                double error = (sigmoid(dot(weights, x[i]) + bias) - y[i]) * sampleWeight;
                for (int j = 0; j < width; j++) {
                    gradient[j] += error * x[i][j];
                }
                biasGradient += error;
            }
            for (int j = 0; j < width; j++) {
                weights[j] -= options.learningRate() * (gradient[j] / totalWeight + options.l2Penalty() * weights[j]);
            }
            bias -= options.learningRate() * biasGradient / totalWeight;
        }
        return new LogisticRegression(weights, bias);
    }

    public double probability(double[] x) {
        return sigmoid(dot(weights, x) + bias);
    }

    public double[] contributions(double[] x) {
        double[] parts = new double[x.length];
        for (int j = 0; j < x.length; j++) {
            parts[j] = weights[j] * x[j];
        }
        return parts;
    }

    public double[] weights() {
        return weights.clone();
    }

    static double sigmoid(double z) {
        return 1.0 / (1.0 + Math.exp(-Math.max(-35, Math.min(35, z))));
    }

    static double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
}
