package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TranscriptSegmentSaveResponse {

    @JsonProperty("practice_id")
    private Long practiceId;

    @JsonProperty("segment_order")
    private Integer segmentOrder;

    @JsonProperty("duration_seconds")
    private Integer durationSeconds;

    @JsonProperty("total_duration_seconds")
    private Integer totalDurationSeconds;

    @JsonProperty("combined_transcript")
    private String combinedTranscript;

    public TranscriptSegmentSaveResponse() {
        // Default constructor for Jackson
    }

    public TranscriptSegmentSaveResponse(Long practiceId,
                                         Integer segmentOrder,
                                         Integer durationSeconds,
                                         Integer totalDurationSeconds,
                                         String combinedTranscript) {
        this.practiceId = practiceId;
        this.segmentOrder = segmentOrder;
        this.durationSeconds = durationSeconds;
        this.totalDurationSeconds = totalDurationSeconds;
        this.combinedTranscript = combinedTranscript;
    }

    public Long getPracticeId() {
        return practiceId;
    }

    public Integer getSegmentOrder() {
        return segmentOrder;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Integer getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public String getCombinedTranscript() {
        return combinedTranscript;
    }
}
