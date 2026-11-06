package com.notsureance.claims.ml.core;

import java.util.Random;

public final class Shuffle {

    private Shuffle() {
    }

    public static int[] indices(int size, long seed) {
        int[] order = new int[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        Random random = new Random(seed);
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = order[i];
            order[i] = order[j];
            order[j] = swap;
        }
        return order;
    }
}
