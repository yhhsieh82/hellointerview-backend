package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TranscriptSegmentSaveRequest {

    @JsonProperty("transcript_text")
    private String transcriptText;

    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    public TranscriptSegmentSaveRequest() {
        // Default constructor for Jackson
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
