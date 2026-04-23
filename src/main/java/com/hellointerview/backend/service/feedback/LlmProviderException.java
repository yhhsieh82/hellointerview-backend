package com.hellointerview.backend.service.feedback;

public class LlmProviderException extends RuntimeException {

    private final boolean transientFailure;

    public LlmProviderException(String message, boolean transientFailure) {
        super(message);
        this.transientFailure = transientFailure;
    }

    public LlmProviderException(String message, boolean transientFailure, Throwable cause) {
        super(message, cause);
        this.transientFailure = transientFailure;
    }

    public boolean isTransientFailure() {
        return transientFailure;
    }
}
