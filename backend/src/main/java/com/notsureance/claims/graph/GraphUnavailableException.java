package com.notsureance.claims.graph;

public class GraphUnavailableException extends RuntimeException {

    public GraphUnavailableException(Throwable cause) {
        super("The fraud graph is not reachable right now. Try again in a minute.", cause);
    }
}
