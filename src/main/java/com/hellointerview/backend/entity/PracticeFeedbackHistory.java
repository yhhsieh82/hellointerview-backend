package com.hellointerview.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "practice_feedback_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeFeedbackHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_feedback_id")
    private Long practiceFeedbackId;

    @ManyToOne
    @JoinColumn(name = "practice_id", nullable = false)
    private PracticeHistory practice;

    @Column(name = "feedback_text", nullable = false)
    private String feedbackText;

    @Column(name = "score")
    private Double score;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;
}

