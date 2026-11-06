package com.notsureance.claims.ml.scoring;

import com.notsureance.claims.ml.core.LogisticRegression;
import com.notsureance.claims.ml.core.NaiveBayesTextClassifier;
import com.notsureance.claims.ml.core.RidgeRegression;
import com.notsureance.claims.ml.core.StandardScaler;

public record TrainedModels(
        StandardScaler fraudScaler,
        LogisticRegression fraudModel,
        StandardScaler payoutScaler,
        RidgeRegression payoutModel,
        NaiveBayesTextClassifier causeModel,
        AnomalyBaseline anomalyBaseline,
        ModelReport report) {
}
