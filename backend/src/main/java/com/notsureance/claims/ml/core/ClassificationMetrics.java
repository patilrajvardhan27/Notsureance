package com.notsureance.claims.ml.core;

import java.util.Arrays;
import java.util.Comparator;

public record ClassificationMetrics(double accuracy, double precision, double recall, double f1, double rocAuc,
                                    int truePositives, int falsePositives, int trueNegatives, int falseNegatives) {

    public static ClassificationMetrics evaluate(double[] scores, int[] labels, double threshold) {
        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;
        for (int i = 0; i < scores.length; i++) {
            boolean predicted = scores[i] >= threshold;
            boolean actual = labels[i] == 1;
            if (predicted && actual) {
                tp++;
            } else if (predicted) {
                fp++;
            } else if (actual) {
                fn++;
            } else {
                tn++;
            }
        }
        double precision = tp + fp == 0 ? 0 : tp / (double) (tp + fp);
        double recall = tp + fn == 0 ? 0 : tp / (double) (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        double accuracy = (tp + tn) / (double) scores.length;
        return new ClassificationMetrics(accuracy, precision, recall, f1, rocAuc(scores, labels), tp, fp, tn, fn);
    }

    static double rocAuc(double[] scores, int[] labels) {
        Integer[] order = new Integer[scores.length];
        Arrays.setAll(order, i -> i);
        Arrays.sort(order, Comparator.comparingDouble(i -> scores[i]));
        double[] ranks = new double[scores.length];
        for (int i = 0; i < order.length; ) {
            int j = i;
            while (j + 1 < order.length && scores[order[j + 1]] == scores[order[i]]) {
                j++;
            }
            double averageRank = (i + j) / 2.0 + 1;
            for (int k = i; k <= j; k++) {
                ranks[order[k]] = averageRank;
            }
            i = j + 1;
        }
        double positives = Arrays.stream(labels).sum();
        double negatives = labels.length - positives;
        if (positives == 0 || negatives == 0) {
            return 0.5;
        }
        double positiveRankSum = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == 1) {
                positiveRankSum += ranks[i];
            }
        }
        return (positiveRankSum - positives * (positives + 1) / 2) / (positives * negatives);
    }
}
