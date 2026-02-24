package com.hellointerview.backend.controller;

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

        practicingSession = PracticeMain.builder()
                .practiceMainId(123L)
                .userId(456L)
                .questionMainId(1L)
                .status("practicing")
                .startedAt(startedAt)
                .completedAt(null)
                .build();

        completedSession = PracticeMain.builder()
                .practiceMainId(123L)
                .userId(456L)
                .questionMainId(1L)
                .status("completed")
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();
    }

    @Test
    void getActivePracticeMain_WhenExists_Returns200WithBody() throws Exception {
        when(practiceMainService.getActivePracticeMain(456L, 1L, "practicing"))
                .thenReturn(practicingSession);

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
                .andExpect(jsonPath("$.completed_at").doesNotExist());

        verify(practiceMainService, times(1))
                .getActivePracticeMain(456L, 1L, "practicing");
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

        verify(practiceMainService, never()).getActivePracticeMain(any(), any(), any());
    }
}

