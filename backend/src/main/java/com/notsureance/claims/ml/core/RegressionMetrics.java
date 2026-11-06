package com.notsureance.claims.ml.core;

public record RegressionMetrics(double meanAbsoluteError, double rootMeanSquaredError, double rSquared) {

    public static RegressionMetrics evaluate(double[] predicted, double[] actual) {
        double mean = 0;
        for (double value : actual) {
            mean += value / actual.length;
        }
        double absolute = 0;
        double squared = 0;
        double total = 0;
        for (int i = 0; i < actual.length; i++) {
            absolute += Math.abs(predicted[i] - actual[i]);
            squared += Math.pow(predicted[i] - actual[i], 2);
            total += Math.pow(actual[i] - mean, 2);
        }
        double rSquared = total == 0 ? 0 : 1 - squared / total;
        return new RegressionMetrics(absolute / actual.length, Math.sqrt(squared / actual.length), rSquared);
    }
}
