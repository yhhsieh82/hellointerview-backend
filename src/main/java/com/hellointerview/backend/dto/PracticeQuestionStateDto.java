package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PracticeQuestionStateDto {

    @JsonProperty("practice_id")
    private Long practiceId;

    @JsonProperty("practice_main_id")
    private Long practiceMainId;

    @JsonProperty("question_id")
    private Long questionId;

    @JsonProperty("transcript_segments")
    private List<TranscriptSegmentDto> transcriptSegments;

    @JsonProperty("total_duration_seconds")
    private Integer totalDurationSeconds;

    @JsonProperty("combined_transcript")
    private String combinedTranscript;

    public PracticeQuestionStateDto() {
        // Default constructor for Jackson
    }

    public PracticeQuestionStateDto(Long practiceId,
                                    Long practiceMainId,
                                    Long questionId,
                                    List<TranscriptSegmentDto> transcriptSegments,
                                    Integer totalDurationSeconds,
                                    String combinedTranscript) {
        this.practiceId = practiceId;
        this.practiceMainId = practiceMainId;
        this.questionId = questionId;
        this.transcriptSegments = transcriptSegments;
        this.totalDurationSeconds = totalDurationSeconds;
        this.combinedTranscript = combinedTranscript;
    }

    public Long getPracticeId() {
        return practiceId;
    }

    public Long getPracticeMainId() {
        return practiceMainId;
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
