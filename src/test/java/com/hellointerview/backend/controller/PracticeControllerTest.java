package com.hellointerview.backend.controller;

import com.hellointerview.backend.dto.PracticeTranscriptStateResponse;
import com.hellointerview.backend.dto.TranscriptSegmentDto;
import com.hellointerview.backend.dto.TranscriptSegmentSaveResponse;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.service.PracticeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PracticeController.class)
class PracticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PracticeService practiceService;

    @Test
    void saveTranscriptSegment_WhenValid_Returns200() throws Exception {
        TranscriptSegmentSaveResponse response = new TranscriptSegmentSaveResponse(
                789L,
                3,
                95,
                245,
                "I would start with requirements. Then add a load balancer."
        );
        when(practiceService.saveTranscriptSegment(eq(789L), any())).thenReturn(response);

        String requestBody = """
                {
                  "transcript_text": "Then add a load balancer.",
                  "duration_seconds": 95
                }
                """;

        mockMvc.perform(post("/api/v1/practice/{practiceId}/transcript-segments", 789L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.practice_id").value(789))
                .andExpect(jsonPath("$.segment_order").value(3))
                .andExpect(jsonPath("$.duration_seconds").value(95))
                .andExpect(jsonPath("$.total_duration_seconds").value(245))
                .andExpect(jsonPath("$.combined_transcript").value("I would start with requirements. Then add a load balancer."));
    }

    @Test
    void saveTranscriptSegment_WhenValidationFails_Returns400() throws Exception {
        when(practiceService.saveTranscriptSegment(eq(789L), any()))
                .thenThrow(new BadRequestException("Transcript text cannot be empty"));

        String requestBody = """
                {
                  "transcript_text": "",
                  "duration_seconds": 95
                }
                """;

        mockMvc.perform(post("/api/v1/practice/{practiceId}/transcript-segments", 789L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void saveTranscriptSegment_WhenPracticeMissing_Returns404() throws Exception {
        when(practiceService.saveTranscriptSegment(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Practice with id 999 does not exist"));

        String requestBody = """
                {
                  "transcript_text": "segment",
                  "duration_seconds": 10
                }
                """;

        mockMvc.perform(post("/api/v1/practice/{practiceId}/transcript-segments", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource not found"));
    }

    @Test
    void getPracticeTranscriptState_WhenValid_Returns200() throws Exception {
        PracticeTranscriptStateResponse response = new PracticeTranscriptStateResponse(
                789L,
                456L,
                List.of(
                        new TranscriptSegmentDto(1, "First segment", 80),
                        new TranscriptSegmentDto(2, "Second segment", 70)
                ),
                150,
                "First segment Second segment"
        );
        when(practiceService.getPracticeTranscriptState(789L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/practice/{practiceId}", 789L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.practice_id").value(789))
                .andExpect(jsonPath("$.question_id").value(456))
                .andExpect(jsonPath("$.transcript_segments[0].segment_order").value(1))
                .andExpect(jsonPath("$.transcript_segments[1].segment_order").value(2))
                .andExpect(jsonPath("$.total_duration_seconds").value(150))
                .andExpect(jsonPath("$.combined_transcript").value("First segment Second segment"));
    }
}
