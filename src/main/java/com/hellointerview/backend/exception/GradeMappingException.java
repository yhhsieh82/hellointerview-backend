package com.hellointerview.backend.exception;

/**
 * Thrown when a persisted or computed score cannot be mapped to API grade fields (product PRD §2.4).
 */
public class GradeMappingException extends RuntimeException {

    public GradeMappingException(String message) {
        super(message);
    }
}
