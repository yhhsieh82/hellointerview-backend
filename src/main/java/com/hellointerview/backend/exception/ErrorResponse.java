package com.hellointerview.backend.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standard error response structure for API errors.
 */
public class ErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("details")
    private List<ValidationErrorDetail> details;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("code")
    private String code;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.details = List.of();
    }

    public ErrorResponse(String error, String message, String code) {
        this.error = error;
        this.message = message;
        this.code = code;
        this.details = List.of();
    }

    public ErrorResponse(String error, String message, List<ValidationErrorDetail> details) {
        this.error = error;
        this.message = message;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ValidationErrorDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ValidationErrorDetail> details) {
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
