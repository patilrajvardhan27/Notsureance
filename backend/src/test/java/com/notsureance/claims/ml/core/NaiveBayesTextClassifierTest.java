package com.notsureance.claims.ml.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NaiveBayesTextClassifierTest {

    private final NaiveBayesTextClassifier model = NaiveBayesTextClassifier.fit(
            List.of("hail dented the hood", "hailstorm cracked windshield", "fire in the engine bay",
                    "flames and smoke from exhaust", "stolen from the yard", "tractor taken overnight lock cut"),
            List.of("HAIL", "HAIL", "FIRE", "FIRE", "THEFT", "THEFT"), 1.0);

    @Test
    void predictsTheMostLikelyCause() {
        assertThat(model.predict("smoke and fire near the engine").label()).isEqualTo("FIRE");
        assertThat(model.predict("lock was cut and it was stolen").label()).isEqualTo("THEFT");
    }

    @Test
    void probabilitiesFormADistribution() {
        var prediction = model.predict("hail cracked the cab");
        double total = prediction.probabilities().values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(total).isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-9));
        assertThat(prediction.probabilityOf("HAIL")).isGreaterThan(prediction.probabilityOf("THEFT"));
    }

    @Test
    void tokenizerDropsStopWordsAndPlurals() {
        assertThat(NaiveBayesTextClassifier.tokenize("The tires were slashed!")).containsExactly("tire", "slashed");
    }
}
