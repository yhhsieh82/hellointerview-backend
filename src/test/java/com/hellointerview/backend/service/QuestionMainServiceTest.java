package com.hellointerview.backend.service;

import com.hellointerview.backend.entity.Question;
import com.hellointerview.backend.entity.QuestionMain;
import com.hellointerview.backend.entity.QuestionType;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.QuestionMainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionMainServiceTest {

    @Mock
    private QuestionMainRepository questionMainRepository;

    @InjectMocks
    private QuestionMainService questionMainService;

    private QuestionMain mockQuestionMain;
    private List<Question> mockQuestions;

    @BeforeEach
    void setUp() {
        // Create mock QuestionMain
        mockQuestionMain = QuestionMain.builder()
                .questionMainId(1L)
                .name("Design Twitter")
                .description("Design a social media platform")
                .writeUp("# Sample Answer\nDetailed explanation...")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .questions(new ArrayList<>())
                .build();

        // Create mock Questions
        mockQuestions = List.of(
                Question.builder()
                        .questionId(1L)
                        .questionMainId(1L)
                        .order(1)
                        .type(QuestionType.FUNCTIONAL_REQ)
                        .name("Define Functional Requirements")
                        .description("What are the core features?")
                        .whiteboardSection(1)
                        .requiresRecording(false)
                        .createdAt(Instant.now())
                        .build(),
                Question.builder()
                        .questionId(2L)
                        .questionMainId(1L)
                        .order(2)
                        .type(QuestionType.HIGH_LEVEL_DESIGN)
                        .name("Design System Architecture")
                        .description("Create high-level design")
                        .whiteboardSection(5)
                        .requiresRecording(true)
                        .createdAt(Instant.now())
                        .build()
        );

        mockQuestionMain.setQuestions(mockQuestions);
    }

    @Test
    void getQuestionMainById_WhenExists_ReturnsQuestionMainWithQuestions() {
        // Given
        Long questionMainId = 1L;
        when(questionMainRepository.findByIdWithQuestions(questionMainId))
                .thenReturn(Optional.of(mockQuestionMain));

        // When
        QuestionMain result = questionMainService.getQuestionMainById(questionMainId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQuestionMainId()).isEqualTo(questionMainId);
        assertThat(result.getName()).isEqualTo("Design Twitter");
        assertThat(result.getDescription()).isEqualTo("Design a social media platform");
        assertThat(result.getWriteUp()).isEqualTo("# Sample Answer\nDetailed explanation...");
        assertThat(result.getQuestions()).hasSize(2);
        
        // Verify questions are ordered correctly
        assertThat(result.getQuestions().get(0).getOrder()).isEqualTo(1);
        assertThat(result.getQuestions().get(0).getType()).isEqualTo(QuestionType.FUNCTIONAL_REQ);
        assertThat(result.getQuestions().get(1).getOrder()).isEqualTo(2);
        assertThat(result.getQuestions().get(1).getType()).isEqualTo(QuestionType.HIGH_LEVEL_DESIGN);
        assertThat(result.getQuestions().get(1).getRequiresRecording()).isTrue();

        // Verify repository was called exactly once
        verify(questionMainRepository, times(1)).findByIdWithQuestions(questionMainId);
    }

    @Test
    void getQuestionMainById_WhenNotFound_ThrowsResourceNotFoundException() {
        // Given
        Long questionMainId = 999L;
        when(questionMainRepository.findByIdWithQuestions(questionMainId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> questionMainService.getQuestionMainById(questionMainId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("QuestionMain with id " + questionMainId + " does not exist");

        // Verify repository was called
        verify(questionMainRepository, times(1)).findByIdWithQuestions(questionMainId);
    }

    @Test
    void getQuestionMainById_WithEmptyQuestionsList_ReturnsQuestionMainWithNoQuestions() {
        // Given
        Long questionMainId = 1L;
        QuestionMain emptyQuestionsMain = QuestionMain.builder()
                .questionMainId(questionMainId)
                .name("Design System")
                .description("A system design challenge")
                .writeUp("Sample answer")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .questions(new ArrayList<>())
                .build();

        when(questionMainRepository.findByIdWithQuestions(questionMainId))
                .thenReturn(Optional.of(emptyQuestionsMain));

        // When
        QuestionMain result = questionMainService.getQuestionMainById(questionMainId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQuestionMainId()).isEqualTo(questionMainId);
        assertThat(result.getQuestions()).isEmpty();

        verify(questionMainRepository, times(1)).findByIdWithQuestions(questionMainId);
    }
}
