package com.notsureance.claims.ml.core;

import java.util.Random;

public final class KMeans {

    private KMeans() {
    }

    public record Result(double[][] centroids, int[] assignments, double inertia) {
    }

    public static Result fit(double[][] points, int k, int maxIterations, long seed) {
        int clusters = Math.min(k, points.length);
        double[][] centroids = seedCentroids(points, clusters, new Random(seed));
        int[] assignments = new int[points.length];
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            boolean changed = false;
            for (int i = 0; i < points.length; i++) {
                int nearest = nearest(points[i], centroids);
                if (nearest != assignments[i]) {
                    assignments[i] = nearest;
                    changed = true;
                }
            }
            centroids = recompute(points, assignments, centroids);
            if (!changed && iteration > 0) {
                break;
            }
        }
        double inertia = 0;
        for (int i = 0; i < points.length; i++) {
            inertia += squaredDistance(points[i], centroids[assignments[i]]);
        }
        return new Result(centroids, assignments, inertia);
    }

    public static double squaredDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return sum;
    }

    private static double[][] seedCentroids(double[][] points, int k, Random random) {
        double[][] centroids = new double[k][];
        centroids[0] = points[random.nextInt(points.length)].clone();
        for (int c = 1; c < k; c++) {
            double[] distances = new double[points.length];
            double total = 0;
            for (int i = 0; i < points.length; i++) {
                double best = Double.MAX_VALUE;
                for (int j = 0; j < c; j++) {
                    best = Math.min(best, squaredDistance(points[i], centroids[j]));
                }
                distances[i] = best;
                total += best;
            }
            double target = random.nextDouble() * total;
            int chosen = 0;
            for (double running = distances[0]; running < target && chosen < points.length - 1; ) {
                running += distances[++chosen];
            }
            centroids[c] = points[chosen].clone();
        }
        return centroids;
    }

    private static int nearest(double[] point, double[][] centroids) {
        int best = 0;
        for (int c = 1; c < centroids.length; c++) {
            if (squaredDistance(point, centroids[c]) < squaredDistance(point, centroids[best])) {
                best = c;
            }
        }
        return best;
    }

    private static double[][] recompute(double[][] points, int[] assignments, double[][] previous) {
        int width = points[0].length;
        double[][] sums = new double[previous.length][width];
        int[] counts = new int[previous.length];
        for (int i = 0; i < points.length; i++) {
            counts[assignments[i]]++;
            for (int j = 0; j < width; j++) {
                sums[assignments[i]][j] += points[i][j];
            }
        }
        for (int c = 0; c < previous.length; c++) {
            if (counts[c] == 0) {
                sums[c] = previous[c].clone();
                continue;
            }
            for (int j = 0; j < width; j++) {
                sums[c][j] /= counts[c];
            }
        }
        return sums;
    }
}
