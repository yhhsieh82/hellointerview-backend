package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PracticeSubmitResponse {

    @JsonProperty("practice_id")
    private final Long practiceId;

    @JsonProperty("practice_main_id")
    private final Long practiceMainId;

    @JsonProperty("question_id")
    private final Long questionId;

    @JsonProperty("audio_required")
    private final boolean audioRequired;

    @JsonProperty("accepted_combined_transcript")
    private final String acceptedCombinedTranscript;

    @JsonProperty("accepted_total_duration_seconds")
    private final Integer acceptedTotalDurationSeconds;

    public PracticeSubmitResponse(Long practiceId,
                                  Long practiceMainId,
                                  Long questionId,
                                  boolean audioRequired,
                                  String acceptedCombinedTranscript,
                                  Integer acceptedTotalDurationSeconds) {
        this.practiceId = practiceId;
        this.practiceMainId = practiceMainId;
        this.questionId = questionId;
        this.audioRequired = audioRequired;
        this.acceptedCombinedTranscript = acceptedCombinedTranscript;
        this.acceptedTotalDurationSeconds = acceptedTotalDurationSeconds;
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

    public boolean isAudioRequired() {
        return audioRequired;
    }

    public String getAcceptedCombinedTranscript() {
        return acceptedCombinedTranscript;
    }

    public Integer getAcceptedTotalDurationSeconds() {
        return acceptedTotalDurationSeconds;
    }
}
