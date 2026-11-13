package com.notsureance.claims.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Used when RAG_EMBEDDER=spring-ai, backed by the Azure OpenAI embedding deployment set to 384 dimensions.
@Component
@ConditionalOnProperty(name = "app.rag.embedder", havingValue = "spring-ai")
public class SpringAiTextEmbedder implements TextEmbedder {

    private final EmbeddingModel model;

    public SpringAiTextEmbedder(EmbeddingModel model) {
        this.model = model;
    }

    @Override
    public String name() {
        return "spring-ai-384";
    }

    @Override
    public float[] embed(String text) {
        float[] vector = model.embed(text);
        if (vector.length != DIMENSIONS) {
            throw new IllegalStateException("Embedding model returned " + vector.length + " dimensions, expected "
                    + DIMENSIONS + ". Set the deployment's dimensions to " + DIMENSIONS + ".");
        }
        return vector;
    }
}
