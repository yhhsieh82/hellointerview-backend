package com.hellointerview.backend.controller;

import com.hellointerview.backend.dto.PracticeMainResponseDto;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.service.PracticeMainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PracticeMainController.class)
class PracticeMainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PracticeMainService practiceMainService;

    private PracticeMain practicingSession;
    private PracticeMain completedSession;
    private Instant startedAt;
    private Instant completedAt;

    @BeforeEach
    void setUp() {
        startedAt = Instant.parse("2026-02-13T09:00:00Z");
        completedAt = Instant.parse("2026-02-13T11:30:00Z");

        practicingSession = new PracticeMain();
        practicingSession.setPracticeMainId(123L);
        practicingSession.setUserId(456L);
        practicingSession.setQuestionMainId(1L);
        practicingSession.setStatus("practicing");
        practicingSession.setStartedAt(startedAt);

        completedSession = new PracticeMain();
        completedSession.setPracticeMainId(123L);
        completedSession.setUserId(456L);
        completedSession.setQuestionMainId(1L);
        completedSession.setStatus("completed");
        completedSession.setStartedAt(startedAt);
        completedSession.setCompletedAt(completedAt);
    }

    @Test
    void getActivePracticeMain_WhenExists_Returns200WithBodyIncludingProgress() throws Exception {
        PracticeMainResponseDto responseDto = new PracticeMainResponseDto(
                123L,
                456L,
                1L,
                "practicing",
                startedAt,
                null,
                List.of(10L, 20L)
        );

        when(practiceMainService.getActivePracticeMainWithProgress(456L, 1L, "practicing"))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/practice-main")
                        .param("user_id", "456")
                        .param("question_main_id", "1")
                        .param("status", "practicing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.practice_main_id").value(123))
                .andExpect(jsonPath("$.user_id").value(456))
                .andExpect(jsonPath("$.question_main_id").value(1))
                .andExpect(jsonPath("$.status").value("practicing"))
                .andExpect(jsonPath("$.started_at").value("2026-02-13T09:00:00Z"))
                .andExpect(jsonPath("$.completed_at").doesNotExist())
                .andExpect(jsonPath("$.question_ids_with_practices[0]").value(10))
                .andExpect(jsonPath("$.question_ids_with_practices[1]").value(20));

        verify(practiceMainService, times(1))
                .getActivePracticeMainWithProgress(456L, 1L, "practicing");
    }

    @Test
    void createPracticeMain_WhenValid_Returns201WithBody() throws Exception {
        when(practiceMainService.createPracticeMain(456L, 1L))
                .thenReturn(practicingSession);

        String requestBody = """
                {
                  "question_main_id": 1,
                  "user_id": 456
                }
                """;

        mockMvc.perform(post("/api/v1/practice-main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.practice_main_id").value(123))
                .andExpect(jsonPath("$.user_id").value(456))
                .andExpect(jsonPath("$.question_main_id").value(1))
                .andExpect(jsonPath("$.status").value("practicing"))
                .andExpect(jsonPath("$.started_at").value("2026-02-13T09:00:00Z"))
                .andExpect(jsonPath("$.completed_at").doesNotExist());

        verify(practiceMainService, times(1))
                .createPracticeMain(456L, 1L);
    }

    @Test
    void updatePracticeMainStatus_WhenCompleted_Returns200WithCompletedSession() throws Exception {
        when(practiceMainService.updatePracticeMainStatus(eq(123L), eq("completed")))
                .thenReturn(completedSession);

        String requestBody = """
                {
                  "status": "completed"
                }
                """;

        mockMvc.perform(patch("/api/v1/practice-main/{id}", 123L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.practice_main_id").value(123))
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completed_at").value("2026-02-13T11:30:00Z"));

        verify(practiceMainService, times(1))
                .updatePracticeMainStatus(123L, "completed");
    }

    @Test
    void getActivePracticeMain_WithInvalidUserIdFormat_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/practice-main")
                        .param("user_id", "invalid")
                        .param("question_main_id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(practiceMainService, never()).getActivePracticeMainWithProgress(any(), any(), any());
    }
}

