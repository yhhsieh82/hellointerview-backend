package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class QuestionMainSummaryDto {

    @JsonProperty("question_main_id")
    private Long questionMainId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    public QuestionMainSummaryDto() {
    }

    public QuestionMainSummaryDto(Long questionMainId, String name, String description,
                                  Instant createdAt, Instant updatedAt) {
        this.questionMainId = questionMainId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getQuestionMainId() {
        return questionMainId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
