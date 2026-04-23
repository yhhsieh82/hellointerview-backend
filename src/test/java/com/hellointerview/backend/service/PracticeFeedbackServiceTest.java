package com.hellointerview.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellointerview.backend.dto.FeedbackPayloadDto;
import com.hellointerview.backend.dto.FeedbackSubmitResponseDto;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeFeedback;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.entity.Question;
import com.hellointerview.backend.entity.QuestionType;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.exception.ConflictException;
import com.hellointerview.backend.exception.FeedbackInProgressException;
import com.hellointerview.backend.exception.GradeMappingException;
import com.hellointerview.backend.exception.LlmTimeoutException;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeRepository;
import com.hellointerview.backend.repository.PracticeTranscriptSegmentRepository;
import com.hellointerview.backend.service.feedback.FeedbackClaimResult;
import com.hellointerview.backend.service.feedback.FeedbackIdempotencyCoordinator;
import com.hellointerview.backend.service.feedback.LlmFeedbackClient;
import com.hellointerview.backend.service.feedback.LlmFeedbackInput;
 import com.hellointerview.backend.service.feedback.LlmProviderException;
import com.hellointerview.backend.service.feedback.LlmFeedbackResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeFeedbackServiceTest {

    @Mock
    private PracticeRepository practiceRepository;
    @Mock
    private PracticeTranscriptSegmentRepository transcriptSegmentRepository;
    @Mock
    private FeedbackIdempotencyCoordinator idempotencyCoordinator;
    @Mock
    private LlmFeedbackClient llmFeedbackClient;

    private PracticeFeedbackService service;
    private Practice practice;

    @BeforeEach
    void setUp() {
        service = new PracticeFeedbackService(
                practiceRepository,
                transcriptSegmentRepository,
                idempotencyCoordinator,
                llmFeedbackClient,
                new ObjectMapper()
        );
        practice = buildPracticeWithWhiteboard(789L, 333L, 456L, 1, false);
    }

    @Test
    void submitFeedback_WhenIdempotencyKeyMissing_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.submitFeedback(789L, null));
        verify(practiceRepository, never()).findWithMainAndQuestionById(anyLong());
    }

    @Test
    void submitFeedback_WhenIdempotencyKeyBlank_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.submitFeedback(789L, "   "));
        verify(practiceRepository, never()).findWithMainAndQuestionById(anyLong());
    }

    @Test
    void submitFeedback_WhenClaimReplay_ReturnsWithoutCallingLlm() {
        when(practiceRepository.findWithMainAndQuestionById(789L)).thenReturn(Optional.of(practice));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L)).thenReturn(List.of());
        FeedbackSubmitResponseDto replay = new FeedbackSubmitResponseDto(
                789L,
                new FeedbackPayloadDto(9L, "cached", 80.0, "Strong", "score_strong_green",
                        Instant.parse("2026-02-13T09:00:00Z")),
                Instant.parse("2026-02-13T09:00:00Z")
        );
        when(idempotencyCoordinator.claimOrInsert(eq(456L), eq("k1"), eq(practice), any()))
                .thenReturn(new FeedbackClaimResult.Replay(replay));

        FeedbackSubmitResponseDto dto = service.submitFeedback(789L, "k1");

        assertEquals(9L, dto.feedback().practiceFeedbackId());
        verify(llmFeedbackClient, never()).generate(any());
        verify(idempotencyCoordinator, never()).finalizeSuccessful(anyLong(), any(), any());
    }

    @Test
    void submitFeedback_WhenClaimProceed_CallsFinalize() {
        when(practiceRepository.findWithMainAndQuestionById(789L)).thenReturn(Optional.of(practice));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L)).thenReturn(List.of());
        when(idempotencyCoordinator.claimOrInsert(eq(456L), eq("k2"), eq(practice), any()))
                .thenReturn(new FeedbackClaimResult.Proceed(42L));
        when(llmFeedbackClient.generate(any(LlmFeedbackInput.class))).thenReturn(new LlmFeedbackResult("generated", 60.0));
        PracticeFeedback finalized = PracticeFeedback.builder()
                .practiceFeedbackId(2002L)
                .practice(practice)
                .feedbackText("generated")
                .score(60.0)
                .generatedAt(Instant.parse("2026-02-13T10:05:00Z"))
                .build();
        when(idempotencyCoordinator.finalizeSuccessful(eq(42L), eq(practice), any(LlmFeedbackResult.class)))
                .thenReturn(finalized);

        FeedbackSubmitResponseDto dto = service.submitFeedback(789L, "k2");

        assertEquals(2002L, dto.feedback().practiceFeedbackId());
        verify(idempotencyCoordinator).finalizeSuccessful(eq(42L), eq(practice), any(LlmFeedbackResult.class));
    }

    @Test
    void submitFeedback_WhenConflict_Throws() {
        when(practiceRepository.findWithMainAndQuestionById(789L)).thenReturn(Optional.of(practice));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L)).thenReturn(List.of());
        when(idempotencyCoordinator.claimOrInsert(anyLong(), any(), any(), any()))
                .thenReturn(new FeedbackClaimResult.Conflict("bad"));

        assertThrows(ConflictException.class, () -> service.submitFeedback(789L, "k3"));
        verify(llmFeedbackClient, never()).generate(any());
    }

    @Test
    void submitFeedback_WhenInProgress_Throws() {
        when(practiceRepository.findWithMainAndQuestionById(789L)).thenReturn(Optional.of(practice));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L)).thenReturn(List.of());
        when(idempotencyCoordinator.claimOrInsert(anyLong(), any(), any(), any()))
                .thenReturn(new FeedbackClaimResult.InProgress());

        assertThrows(FeedbackInProgressException.class, () -> service.submitFeedback(789L, "k4"));
        verify(llmFeedbackClient, never()).generate(any());
    }

    @Test
    void submitFeedback_WhenLlmTimeout_MarksFailedAndPropagates() {
        when(practiceRepository.findWithMainAndQuestionById(789L)).thenReturn(Optional.of(practice));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L)).thenReturn(List.of());
        when(idempotencyCoordinator.claimOrInsert(anyLong(), any(), any(), any()))
                .thenReturn(new FeedbackClaimResult.Proceed(99L));
        when(llmFeedbackClient.generate(any(LlmFeedbackInput.class))).thenThrow(new LlmTimeoutException("timed out"));

        assertThrows(LlmTimeoutException.class, () -> service.submitFeedback(789L, "k5"));

        verify(idempotencyCoordinator).markRequestFailed(99L, "llm_timeout");
        verify(idempotencyCoordinator, never()).finalizeSuccessful(anyLong(), any(), any());
    }

    @Test
    void submitFeedback_WhenGradeMappingFailsDuringFinalize_MarksFailedAndPropagates() {
        when(practiceRepository.findWithMainAndQuestionById(789L)).thenReturn(Optional.of(practice));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L)).thenReturn(List.of());
        when(idempotencyCoordinator.claimOrInsert(anyLong(), any(), any(), any()))
                .thenReturn(new FeedbackClaimResult.Proceed(88L));
        when(llmFeedbackClient.generate(any(LlmFeedbackInput.class))).thenReturn(new LlmFeedbackResult("ok", 50.0));
        when(idempotencyCoordinator.finalizeSuccessful(eq(88L), eq(practice), any(LlmFeedbackResult.class)))
                .thenThrow(new GradeMappingException("bad score"));

        assertThrows(GradeMappingException.class, () -> service.submitFeedback(789L, "k-grade"));

        verify(idempotencyCoordinator).markRequestFailed(88L, "grade_mapping_failed");
    }

    @Test
    void submitFeedback_WhenLlmTransientFailure_MarksFailedAndPropagates() {
        when(practiceRepository.findWithMainAndQuestionById(789L)).thenReturn(Optional.of(practice));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L)).thenReturn(List.of());
        when(idempotencyCoordinator.claimOrInsert(anyLong(), any(), any(), any()))
                .thenReturn(new FeedbackClaimResult.Proceed(77L));
        when(llmFeedbackClient.generate(any(LlmFeedbackInput.class)))
                .thenThrow(new LlmProviderException("retry exhausted", true));

        assertThrows(LlmProviderException.class, () -> service.submitFeedback(789L, "k-transient"));

        verify(idempotencyCoordinator).markRequestFailed(77L, "llm_transient_failure");
    }

    @Test
    void submitFeedback_WhenLlmTerminalFailure_MarksFailedAndPropagates() {
        when(practiceRepository.findWithMainAndQuestionById(789L)).thenReturn(Optional.of(practice));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L)).thenReturn(List.of());
        when(idempotencyCoordinator.claimOrInsert(anyLong(), any(), any(), any()))
                .thenReturn(new FeedbackClaimResult.Proceed(66L));
        when(llmFeedbackClient.generate(any(LlmFeedbackInput.class)))
                .thenThrow(new LlmProviderException("bad request", false));

        assertThrows(LlmProviderException.class, () -> service.submitFeedback(789L, "k-terminal"));

        verify(idempotencyCoordinator).markRequestFailed(66L, "llm_terminal_failure");
    }

    @Test
    void submitFeedback_WhenPracticeMissing_ThrowsNotFound() {
        when(practiceRepository.findWithMainAndQuestionById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.submitFeedback(1L, "idem-key"));
    }

    @Test
    void submitFeedback_WhenRecordingRequiredAndNoSegments_ThrowsBadRequest() {
        Practice p = buildPracticeWithWhiteboard(1L, 2L, 3L, 1, true);
        when(practiceRepository.findWithMainAndQuestionById(1L)).thenReturn(Optional.of(p));
        when(transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(1L)).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> service.submitFeedback(1L, "idem-key"));
    }

    private static Practice buildPracticeWithWhiteboard(long practiceId,
                                                        long practiceMainId,
                                                        long questionId,
                                                        int whiteboardSection,
                                                        boolean requiresRecording) {
        PracticeMain main = new PracticeMain();
        main.setPracticeMainId(practiceMainId);
        main.setUserId(456L);
        main.setQuestionMainId(1L);
        main.setStatus("practicing");
        main.setWhiteboardContent(buildWhiteboard(whiteboardSection));

        Question question = Question.builder()
                .questionId(questionId)
                .whiteboardSection(whiteboardSection)
                .requiresRecording(requiresRecording)
                .type(QuestionType.FUNCTIONAL_REQ)
                .description("Describe the system")
                .name("FR")
                .order(1)
                .build();

        return Practice.builder()
                .practiceId(practiceId)
                .practiceMain(main)
                .question(question)
                .build();
    }

    private static Map<String, Object> buildWhiteboard(int section) {
        Map<String, Object> elem = new LinkedHashMap<>();
        elem.put("type", "rectangle");
        elem.put("text", "API Gateway");
        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(elem);
        Map<String, Object> sectionMap = new LinkedHashMap<>();
        sectionMap.put("elements", elements);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("section_" + section, sectionMap);
        return root;
    }
}
