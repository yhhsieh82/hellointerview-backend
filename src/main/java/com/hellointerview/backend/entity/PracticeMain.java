package com.hellointerview.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "practice_main")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}

