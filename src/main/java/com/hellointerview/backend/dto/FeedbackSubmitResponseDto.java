package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record FeedbackSubmitResponseDto(
        @JsonProperty("practice_id") Long practiceId,
        @JsonProperty("feedback") FeedbackPayloadDto feedback,
        @JsonProperty("submitted_at") Instant submittedAt
) {
}
