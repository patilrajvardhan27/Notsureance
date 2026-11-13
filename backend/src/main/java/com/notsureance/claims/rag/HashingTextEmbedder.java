package com.notsureance.claims.rag;

import com.notsureance.claims.ml.core.NaiveBayesTextClassifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// A lexical embedding (hashed unigrams and bigrams) that ran offline and gave the same vectors in every test run.
@Component
@ConditionalOnProperty(name = "app.rag.embedder", havingValue = "hashing", matchIfMissing = true)
public class HashingTextEmbedder implements TextEmbedder {

    @Override
    public String name() {
        return "hashing-v1";
    }

    @Override
    public float[] embed(String text) {
        List<String> tokens = NaiveBayesTextClassifier.tokenize(text);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            counts.merge(tokens.get(i), 1, Integer::sum);
            if (i + 1 < tokens.size()) {
                counts.merge(tokens.get(i) + "_" + tokens.get(i + 1), 1, Integer::sum);
            }
        }
        float[] vector = new float[DIMENSIONS];
        counts.forEach((term, count) -> {
            int hash = mix(term.hashCode());
            int slot = Math.floorMod(hash, DIMENSIONS);
            float sign = (hash & 0x40000000) == 0 ? 1f : -1f;
            float weight = (float) Math.log1p(count) * (term.contains("_") ? 0.7f : 1f);
            vector[slot] += sign * weight;
        });
        return normalize(vector);
    }

    static float[] normalize(float[] vector) {
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0) {
            return vector;
        }
        float scale = (float) (1 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
        return vector;
    }

    private static int mix(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        return h ^ (h >>> 16);
    }
}
