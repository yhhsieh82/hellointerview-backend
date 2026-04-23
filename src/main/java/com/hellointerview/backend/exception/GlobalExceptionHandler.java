package com.hellointerview.backend.exception;

import com.hellointerview.backend.service.feedback.LlmProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error response format across the API.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle ResourceNotFoundException (404 Not Found)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("Resource not found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle MethodArgumentTypeMismatchException (400 Bad Request)
     * Occurs when path variable or request param type conversion fails
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        logger.warn("Invalid argument type: {}", ex.getMessage());
        String message = String.format("Invalid value '%s' for parameter '%s'", 
                ex.getValue(), ex.getName());
        ErrorResponse errorResponse = new ErrorResponse("Bad request", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle BadRequestException (400 Bad Request)
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("Validation failed", ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        logger.warn("Missing request header: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("Validation failed", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        logger.warn("Conflict: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("Conflict", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(FeedbackInProgressException.class)
    public ResponseEntity<ErrorResponse> handleFeedbackInProgress(FeedbackInProgressException ex) {
        logger.warn("Feedback in progress: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("Service unavailable", ex.getMessage(), "feedback_in_progress");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).headers(headers).body(errorResponse);
    }

    @ExceptionHandler(LlmTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleLlmTimeout(LlmTimeoutException ex) {
        logger.warn("LLM timeout: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("Service unavailable", ex.getMessage(), "llm_timeout");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(LlmProviderException.class)
    public ResponseEntity<ErrorResponse> handleLlmProviderException(LlmProviderException ex) {
        if (ex.isTransientFailure()) {
            logger.warn("LLM transient provider failure: {}", ex.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(
                    "Service unavailable",
                    "Feedback service temporarily unavailable. Please try again.",
                    "llm_transient_failure"
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
        logger.error("LLM terminal provider failure: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                "Internal server error",
                "Feedback generation failed due to provider configuration or request issues.",
                "llm_terminal_failure"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(GradeMappingException.class)
    public ResponseEntity<ErrorResponse> handleGradeMapping(GradeMappingException ex) {
        logger.error("Grade mapping failed: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("Internal server error", ex.getMessage(), "grade_mapping_failed");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle generic exceptions (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        ErrorResponse errorResponse = new ErrorResponse(
                "Internal server error", 
                "An unexpected error occurred. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
