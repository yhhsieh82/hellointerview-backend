package com.hellointerview.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "practice_main_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeMainHistory {

    @Id
    @Column(name = "practice_main_id")
    private Long practiceMainId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_main_id", nullable = false)
    private Long questionMainId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}

