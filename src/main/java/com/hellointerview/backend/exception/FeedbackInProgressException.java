package com.hellointerview.backend.exception;

public class FeedbackInProgressException extends RuntimeException {

    private final int retryAfterSeconds;

    public FeedbackInProgressException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
