package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FeedbackGradeView(
        @JsonProperty("grade_label") String gradeLabel,
        @JsonProperty("grade_color") String gradeColor
) {
}
