package com.hellointerview.backend.exception;

import java.util.List;

public class BadRequestException extends RuntimeException {

    private final List<ValidationErrorDetail> details;

    public BadRequestException(String message) {
        super(message);
        this.details = List.of();
    }

    public BadRequestException(String message, List<ValidationErrorDetail> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ValidationErrorDetail> getDetails() {
        return details;
    }
}
