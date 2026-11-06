package com.notsureance.claims.ml.scoring;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class ModelRegistry {

    private final AtomicReference<TrainedModels> current = new AtomicReference<>();

    public Optional<TrainedModels> current() {
        return Optional.ofNullable(current.get());
    }

    void publish(TrainedModels models) {
        current.set(models);
    }
}
