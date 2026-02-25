package com.hellointerview.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "practice_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeHistory {

    @Id
    @Column(name = "practice_id")
    private Long practiceId;

    @ManyToOne
    @JoinColumn(name = "practice_main_id", nullable = false)
    private PracticeMainHistory practiceMainHistory;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;
}

