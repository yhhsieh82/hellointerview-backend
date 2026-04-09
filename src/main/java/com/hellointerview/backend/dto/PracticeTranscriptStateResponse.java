package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PracticeTranscriptStateResponse {

    @JsonProperty("practice_id")
    private Long practiceId;

    @JsonProperty("question_id")
    private Long questionId;

    @JsonProperty("transcript_segments")
    private List<TranscriptSegmentDto> transcriptSegments;

    @JsonProperty("total_duration_seconds")
    private Integer totalDurationSeconds;

    @JsonProperty("combined_transcript")
    private String combinedTranscript;

    public PracticeTranscriptStateResponse() {
        // Default constructor for Jackson
    }

    public PracticeTranscriptStateResponse(Long practiceId,
                                           Long questionId,
                                           List<TranscriptSegmentDto> transcriptSegments,
                                           Integer totalDurationSeconds,
                                           String combinedTranscript) {
        this.practiceId = practiceId;
        this.questionId = questionId;
        this.transcriptSegments = transcriptSegments;
        this.totalDurationSeconds = totalDurationSeconds;
        this.combinedTranscript = combinedTranscript;
    }

    public Long getPracticeId() {
        return practiceId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public List<TranscriptSegmentDto> getTranscriptSegments() {
        return transcriptSegments;
    }

    public Integer getTotalDurationSeconds() {
        return totalDurationSeconds;
    }

    public String getCombinedTranscript() {
        return combinedTranscript;
    }
}
