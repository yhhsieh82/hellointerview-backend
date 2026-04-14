package com.hellointerview.backend.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationErrorDetail {

    @JsonProperty("field")
    private String field;

    @JsonProperty("message")
    private String message;

    public ValidationErrorDetail(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}
