package com.notsureance.claims.ml.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NaiveBayesTextClassifier {

    private static final Set<String> STOP_WORDS = Set.of("the", "a", "an", "and", "or", "of", "to", "in", "on", "at",
            "was", "were", "is", "it", "our", "we", "my", "i", "with", "for", "by", "from", "that", "this", "had", "has");

    private final Map<String, Double> logPriors;
    private final Map<String, Map<String, Double>> logLikelihoods;
    private final Map<String, Double> unseenWordLog;

    private NaiveBayesTextClassifier(Map<String, Double> logPriors, Map<String, Map<String, Double>> logLikelihoods,
                                     Map<String, Double> unseenWordLog) {
        this.logPriors = logPriors;
        this.logLikelihoods = logLikelihoods;
        this.unseenWordLog = unseenWordLog;
    }

    public record Prediction(String label, double confidence, Map<String, Double> probabilities) {

        public double probabilityOf(String label) {
            return probabilities.getOrDefault(label, 0.0);
        }
    }

    public static NaiveBayesTextClassifier fit(List<String> documents, List<String> labels, double alpha) {
        Map<String, Integer> docsPerLabel = new HashMap<>();
        Map<String, Map<String, Integer>> wordCounts = new HashMap<>();
        Set<String> vocabulary = new HashSet<>();
        for (int i = 0; i < documents.size(); i++) {
            String label = labels.get(i);
            docsPerLabel.merge(label, 1, Integer::sum);
            Map<String, Integer> counts = wordCounts.computeIfAbsent(label, key -> new HashMap<>());
            for (String token : tokenize(documents.get(i))) {
                counts.merge(token, 1, Integer::sum);
                vocabulary.add(token);
            }
        }
        Map<String, Double> priors = new HashMap<>();
        Map<String, Map<String, Double>> likelihoods = new HashMap<>();
        Map<String, Double> unseen = new HashMap<>();
        for (var entry : wordCounts.entrySet()) {
            String label = entry.getKey();
            double total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            double denominator = total + alpha * vocabulary.size();
            Map<String, Double> logs = new HashMap<>();
            entry.getValue().forEach((word, count) -> logs.put(word, Math.log((count + alpha) / denominator)));
            likelihoods.put(label, logs);
            unseen.put(label, Math.log(alpha / denominator));
            priors.put(label, Math.log(docsPerLabel.get(label) / (double) documents.size()));
        }
        return new NaiveBayesTextClassifier(priors, likelihoods, unseen);
    }

    public Prediction predict(String document) {
        List<String> tokens = tokenize(document);
        Map<String, Double> scores = new LinkedHashMap<>();
        for (String label : logPriors.keySet()) {
            double score = logPriors.get(label);
            Map<String, Double> logs = logLikelihoods.get(label);
            for (String token : tokens) {
                score += logs.getOrDefault(token, unseenWordLog.get(label));
            }
            scores.put(label, score);
        }
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double normalizer = scores.values().stream().mapToDouble(s -> Math.exp(s - max)).sum();
        Map<String, Double> probabilities = new LinkedHashMap<>();
        scores.forEach((label, score) -> probabilities.put(label, Math.exp(score - max) / normalizer));
        var best = probabilities.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        return new Prediction(best.getKey(), best.getValue(), probabilities);
    }

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (raw.length() > 2 && !STOP_WORDS.contains(raw)) {
                tokens.add(raw.endsWith("s") && raw.length() > 4 ? raw.substring(0, raw.length() - 1) : raw);
            }
        }
        return tokens;
    }
}
