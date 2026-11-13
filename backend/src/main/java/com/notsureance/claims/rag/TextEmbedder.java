package com.notsureance.claims.rag;

public interface TextEmbedder {

    int DIMENSIONS = 384;

    String name();

    float[] embed(String text);
}
