package com.hellointerview.backend.controller;

import com.hellointerview.backend.dto.FeedbackPayloadDto;
import com.hellointerview.backend.dto.FeedbackSubmitResponseDto;
import com.hellointerview.backend.exception.GlobalExceptionHandler;
import com.hellointerview.backend.exception.GradeMappingException;
import com.hellointerview.backend.service.PracticeFeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PracticesControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PracticeFeedbackService practiceFeedbackService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PracticesController(practiceFeedbackService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitFeedback_WhenBodyNotEmpty_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/practices/1/feedbacks")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foo\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitFeedback_WhenValid_Returns200() throws Exception {
        FeedbackSubmitResponseDto dto = new FeedbackSubmitResponseDto(
                1L,
                new FeedbackPayloadDto(10L, "text", 85.5, "Strong", "score_strong_green",
                        Instant.parse("2026-02-13T10:00:00Z")),
                Instant.parse("2026-02-13T10:00:00Z")
        );
        when(practiceFeedbackService.submitFeedback(eq(1L), eq("abc"))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/practices/1/feedbacks")
                        .header("Idempotency-Key", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.practice_id", is(1)))
                .andExpect(jsonPath("$.feedback.practice_feedback_id", is(10)));
    }

    @Test
    void submitFeedback_WithoutBody_Returns200() throws Exception {
        FeedbackSubmitResponseDto dto = new FeedbackSubmitResponseDto(
                2L,
                new FeedbackPayloadDto(11L, "t", 50.0, "Developing", "score_developing_yellow",
                        Instant.parse("2026-02-13T10:00:00Z")),
                Instant.parse("2026-02-13T10:00:00Z")
        );
        when(practiceFeedbackService.submitFeedback(eq(2L), eq("idem-2"))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/practices/2/feedbacks")
                        .header("Idempotency-Key", "idem-2"))
                .andExpect(status().isOk());
    }

    @Test
    void submitFeedback_WhenIdempotencyKeyMissing_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/practices/2/feedbacks"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitFeedback_WhenGradeMappingFails_Returns500() throws Exception {
        when(practiceFeedbackService.submitFeedback(eq(3L), eq("idem-3")))
                .thenThrow(new GradeMappingException("Score must be between 0 and 100"));

        mockMvc.perform(post("/api/v1/practices/3/feedbacks")
                        .header("Idempotency-Key", "idem-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("grade_mapping_failed"));
    }
}
