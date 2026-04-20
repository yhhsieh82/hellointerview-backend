package com.hellointerview.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "practice_feedback")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_feedback_id")
    private Long practiceFeedbackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practice_id", nullable = false)
    private Practice practice;

    @Column(name = "feedback_text", nullable = false, columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
