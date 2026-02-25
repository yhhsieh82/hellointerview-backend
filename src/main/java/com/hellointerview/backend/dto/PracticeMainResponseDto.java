package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class PracticeMainResponseDto {

    @JsonProperty("practice_main_id")
    private Long practiceMainId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("question_main_id")
    private Long questionMainId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("started_at")
    private Instant startedAt;

    @JsonProperty("completed_at")
    private Instant completedAt;

    @JsonProperty("question_ids_with_practices")
    private List<Long> questionIdsWithPractices;

    public PracticeMainResponseDto() {
        // Default constructor for Jackson
    }

    public PracticeMainResponseDto(Long practiceMainId,
                                   Long userId,
                                   Long questionMainId,
                                   String status,
                                   Instant startedAt,
                                   Instant completedAt,
                                   List<Long> questionIdsWithPractices) {
        this.practiceMainId = practiceMainId;
        this.userId = userId;
        this.questionMainId = questionMainId;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.questionIdsWithPractices = questionIdsWithPractices;
    }

    public Long getPracticeMainId() {
        return practiceMainId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getQuestionMainId() {
        return questionMainId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<Long> getQuestionIdsWithPractices() {
        return questionIdsWithPractices;
    }

    public void setQuestionIdsWithPractices(List<Long> questionIdsWithPractices) {
        this.questionIdsWithPractices = questionIdsWithPractices;
    }
}

