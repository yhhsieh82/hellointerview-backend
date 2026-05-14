package com.hellointerview.backend.exception;

/**
 * Thrown when Strategy B admission rejects a request before an LLM provider attempt (overload / local capacity).
 */
public class LocalCapacityRejectedException extends RuntimeException {

    private final int retryAfterSeconds;

    public LocalCapacityRejectedException(int retryAfterSeconds) {
        super("Feedback service is at capacity for this workload. Please retry.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
