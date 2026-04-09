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
@Table(name = "practice_transcript_segment_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeTranscriptSegmentHistory {

    @Id
    @Column(name = "segment_id")
    private Long segmentId;

    @ManyToOne
    @JoinColumn(name = "practice_id", nullable = false)
    private PracticeHistory practice;

    @Column(name = "segment_order", nullable = false)
    private Integer segmentOrder;

    @Column(name = "transcript_text", nullable = false)
    private String transcriptText;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
