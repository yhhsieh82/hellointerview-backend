package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PracticeQuestionCreateRequest {

    @JsonProperty("question_id")
    private Long questionId;

    public PracticeQuestionCreateRequest() {
        // Default constructor for Jackson
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
}
