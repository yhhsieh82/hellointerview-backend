package com.hellointerview.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "practice_main")
public class PracticeMain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_main_id")
    @JsonProperty("practice_main_id")
    private Long practiceMainId;

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long userId;

    @Column(name = "question_main_id", nullable = false)
    @JsonProperty("question_main_id")
    private Long questionMainId;

    @Column(name = "status", nullable = false, length = 20)
    @JsonProperty("status")
    private String status;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    @JsonProperty("started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    @JsonProperty("completed_at")
    private Instant completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "whiteboard_content", columnDefinition = "jsonb")
    @JsonProperty("whiteboard_content")
    private Map<String, Object> whiteboardContent;

    public PracticeMain() {
        // Default constructor required by JPA
    }

    public Long getPracticeMainId() {
        return practiceMainId;
    }

    public void setPracticeMainId(Long practiceMainId) {
        this.practiceMainId = practiceMainId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getQuestionMainId() {
        return questionMainId;
    }

    public void setQuestionMainId(Long questionMainId) {
        this.questionMainId = questionMainId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Map<String, Object> getWhiteboardContent() {
        return whiteboardContent;
    }

    public void setWhiteboardContent(Map<String, Object> whiteboardContent) {
        this.whiteboardContent = whiteboardContent;
    }
}

