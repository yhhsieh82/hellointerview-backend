package com.hellointerview.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = false)
public class PracticeSubmitRequest {

    @JsonProperty("practice_id")
    private Long practiceId;

    @JsonProperty("practice_main_id")
    private Long practiceMainId;

    @JsonProperty("question_id")
    private Long questionId;

    @JsonProperty("whiteboard_content")
    private Map<String, Object> whiteboardContent;

    public Long getPracticeId() {
        return practiceId;
    }

    public void setPracticeId(Long practiceId) {
        this.practiceId = practiceId;
    }

    public Long getPracticeMainId() {
        return practiceMainId;
    }

    public void setPracticeMainId(Long practiceMainId) {
        this.practiceMainId = practiceMainId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Map<String, Object> getWhiteboardContent() {
        return whiteboardContent;
    }

    public void setWhiteboardContent(Map<String, Object> whiteboardContent) {
        this.whiteboardContent = whiteboardContent;
    }
}
