package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class PracticeMainUpdateRequest {

    @JsonProperty("status")
    private String status;

    @JsonProperty("whiteboard_content")
    private Map<String, Object> whiteboardContent;

    public PracticeMainUpdateRequest() {
        // Default constructor for Jackson
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getWhiteboardContent() {
        return whiteboardContent;
    }

    public void setWhiteboardContent(Map<String, Object> whiteboardContent) {
        this.whiteboardContent = whiteboardContent;
    }
}

