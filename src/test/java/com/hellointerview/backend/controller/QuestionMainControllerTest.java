package com.hellointerview.backend.controller;

import com.hellointerview.backend.entity.Question;
import com.hellointerview.backend.entity.QuestionMain;
import com.hellointerview.backend.entity.QuestionType;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.service.QuestionMainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionMainController.class)
class QuestionMainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionMainService questionMainService;

    private QuestionMain mockQuestionMain;
    private Instant testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = Instant.parse("2026-02-13T09:00:00Z");

        // Create mock Questions
        List<Question> mockQuestions = List.of(
                Question.builder()
                        .questionId(1L)
                        .questionMainId(1L)
                        .order(1)
                        .type(QuestionType.FUNCTIONAL_REQ)
                        .name("Define Functional Requirements")
                        .description("What are the core features of Twitter?")
                        .whiteboardSection(1)
                        .requiresRecording(false)
                        .createdAt(testTimestamp)
                        .build(),
                Question.builder()
                        .questionId(2L)
                        .questionMainId(1L)
                        .order(2)
                        .type(QuestionType.HIGH_LEVEL_DESIGN)
                        .name("Design System Architecture")
                        .description("Create a high-level system design")
                        .whiteboardSection(5)
                        .requiresRecording(true)
                        .createdAt(testTimestamp)
                        .build()
        );

        // Create mock QuestionMain
        mockQuestionMain = QuestionMain.builder()
                .questionMainId(1L)
                .name("Design Twitter")
                .description("Design a social media platform similar to Twitter...")
                .writeUp("# Sample Answer\n...")
                .createdAt(testTimestamp)
                .updatedAt(testTimestamp)
                .questions(new ArrayList<>(mockQuestions))
                .build();
    }

    @Test
    void getQuestionMain_WhenExists_Returns200WithData() throws Exception {
        // Given
        Long questionMainId = 1L;
        when(questionMainService.getQuestionMainById(questionMainId))
                .thenReturn(mockQuestionMain);

        // When & Then
        mockMvc.perform(get("/api/v1/question-mains/{id}", questionMainId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.question_main_id").value(1))
                .andExpect(jsonPath("$.name").value("Design Twitter"))
                .andExpect(jsonPath("$.description").value("Design a social media platform similar to Twitter..."))
                .andExpect(jsonPath("$.write_up").value("# Sample Answer\n..."))
                .andExpect(jsonPath("$.created_at").value("2026-02-13T09:00:00Z"))
                .andExpect(jsonPath("$.updated_at").value("2026-02-13T09:00:00Z"))
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions", hasSize(2)))
                .andExpect(jsonPath("$.questions[0].question_id").value(1))
                .andExpect(jsonPath("$.questions[0].order").value(1))
                .andExpect(jsonPath("$.questions[0].type").value("Functional Req"))
                .andExpect(jsonPath("$.questions[0].name").value("Define Functional Requirements"))
                .andExpect(jsonPath("$.questions[0].whiteboard_section").value(1))
                .andExpect(jsonPath("$.questions[0].requires_recording").value(false))
                .andExpect(jsonPath("$.questions[1].question_id").value(2))
                .andExpect(jsonPath("$.questions[1].order").value(2))
                .andExpect(jsonPath("$.questions[1].type").value("High Level Design"))
                .andExpect(jsonPath("$.questions[1].whiteboard_section").value(5))
                .andExpect(jsonPath("$.questions[1].requires_recording").value(true));

        verify(questionMainService, times(1)).getQuestionMainById(questionMainId);
    }

    @Test
    void getQuestionMain_WhenNotFound_Returns404WithErrorMessage() throws Exception {
        // Given
        Long questionMainId = 999L;
        when(questionMainService.getQuestionMainById(questionMainId))
                .thenThrow(new ResourceNotFoundException("QuestionMain with id " + questionMainId + " does not exist"));

        // When & Then
        mockMvc.perform(get("/api/v1/question-mains/{id}", questionMainId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value("QuestionMain with id " + questionMainId + " does not exist"));

        verify(questionMainService, times(1)).getQuestionMainById(questionMainId);
    }

    @Test
    void getQuestionMain_WithNoQuestions_Returns200WithEmptyQuestionsArray() throws Exception {
        // Given
        Long questionMainId = 2L;
        QuestionMain emptyQuestionsMain = QuestionMain.builder()
                .questionMainId(questionMainId)
                .name("Design System")
                .description("A challenge")
                .writeUp("Answer")
                .createdAt(testTimestamp)
                .updatedAt(testTimestamp)
                .questions(new ArrayList<>())
                .build();

        when(questionMainService.getQuestionMainById(questionMainId))
                .thenReturn(emptyQuestionsMain);

        // When & Then
        mockMvc.perform(get("/api/v1/question-mains/{id}", questionMainId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.question_main_id").value(2))
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions", hasSize(0)));

        verify(questionMainService, times(1)).getQuestionMainById(questionMainId);
    }

    @Test
    void getQuestionMain_WithInvalidIdFormat_Returns400() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/question-mains/{id}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(questionMainService, never()).getQuestionMainById(any());
    }
}
