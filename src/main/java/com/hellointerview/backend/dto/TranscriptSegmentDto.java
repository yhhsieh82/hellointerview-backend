package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TranscriptSegmentDto {

    @JsonProperty("segment_order")
    private Integer segmentOrder;

    @JsonProperty("transcript_text")
    private String transcriptText;

    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    public TranscriptSegmentDto() {
        // Default constructor for Jackson
    }

    public TranscriptSegmentDto(Integer segmentOrder, String transcriptText, Integer durationSeconds) {
        this.segmentOrder = segmentOrder;
        this.transcriptText = transcriptText;
        this.durationSeconds = durationSeconds;
    }

    public Integer getSegmentOrder() {
        return segmentOrder;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }
}
