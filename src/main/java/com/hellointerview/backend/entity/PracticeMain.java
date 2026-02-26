package com.hellointerview.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

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
}

