package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record FeedbackPayloadDto(
        @JsonProperty("practice_feedback_id") Long practiceFeedbackId,
        @JsonProperty("feedback_text") String feedbackText,
        @JsonProperty("score") Double score,
        @JsonProperty("grade_label") String gradeLabel,
        @JsonProperty("grade_color") String gradeColor,
        @JsonProperty("generated_at") Instant generatedAt
) {
}
