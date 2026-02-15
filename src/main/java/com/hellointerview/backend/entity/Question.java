package com.hellointerview.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "question")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    @JsonProperty("question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_main_id", nullable = false)
    @JsonIgnore
    private QuestionMain questionMain;

    @Column(name = "question_main_id", insertable = false, updatable = false)
    @JsonProperty("question_main_id")
    private Long questionMainId;

    @Column(name = "\"order\"", nullable = false)
    @JsonProperty("order")
    private Integer order;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    @JsonProperty("type")
    private QuestionType type;

    @Column(name = "name", nullable = false, length = 200)
    @JsonProperty("name")
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @JsonProperty("description")
    private String description;

    @Column(name = "whiteboard_section", nullable = false)
    @JsonProperty("whiteboard_section")
    private Integer whiteboardSection;

    @Column(name = "requires_recording", nullable = false)
    @JsonProperty("requires_recording")
    @Builder.Default
    private Boolean requiresRecording = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private Instant createdAt;
}
