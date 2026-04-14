package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    @JsonProperty("question_ids_with_feedback")
    private List<Long> questionIdsWithFeedback;

    @JsonProperty("whiteboard_content")
    private Map<String, Object> whiteboardContent;

    public PracticeMainResponseDto() {
        // Default constructor for Jackson
    }

    public PracticeMainResponseDto(Long practiceMainId,
                                   Long userId,
                                   Long questionMainId,
                                   String status,
                                   Instant startedAt,
                                   Instant completedAt,
                                   List<Long> questionIdsWithFeedback,
                                   Map<String, Object> whiteboardContent) {
        this.practiceMainId = practiceMainId;
        this.userId = userId;
        this.questionMainId = questionMainId;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.questionIdsWithFeedback = questionIdsWithFeedback;
        this.whiteboardContent = whiteboardContent;
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

    public List<Long> getQuestionIdsWithFeedback() {
        return questionIdsWithFeedback;
    }

    public void setQuestionIdsWithFeedback(List<Long> questionIdsWithFeedback) {
        this.questionIdsWithFeedback = questionIdsWithFeedback;
    }

    public Map<String, Object> getWhiteboardContent() {
        return whiteboardContent;
    }

    public void setWhiteboardContent(Map<String, Object> whiteboardContent) {
        this.whiteboardContent = whiteboardContent;
    }
}

