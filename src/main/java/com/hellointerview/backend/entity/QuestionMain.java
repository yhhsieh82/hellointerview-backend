package com.hellointerview.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_main")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionMain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_main_id")
    @JsonProperty("question_main_id")
    private Long questionMainId;

    @Column(name = "name", nullable = false, length = 200)
    @JsonProperty("name")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    @JsonProperty("description")
    private String description;

    @Column(name = "write_up", nullable = false, columnDefinition = "TEXT")
    @JsonProperty("write_up")
    private String writeUp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "questionMain", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("order ASC")
    @JsonProperty("questions")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();
}
