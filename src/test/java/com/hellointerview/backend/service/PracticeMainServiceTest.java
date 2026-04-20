package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.PracticeQuestionStateDto;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeFeedback;
import com.hellointerview.backend.entity.PracticeFeedbackHistory;
import com.hellointerview.backend.entity.PracticeHistory;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.entity.PracticeMainHistory;
import com.hellointerview.backend.entity.PracticeTranscriptSegment;
import com.hellointerview.backend.entity.Question;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeFeedbackHistoryRepository;
import com.hellointerview.backend.repository.PracticeFeedbackRepository;
import com.hellointerview.backend.repository.PracticeHistoryRepository;
import com.hellointerview.backend.repository.PracticeMainHistoryRepository;
import com.hellointerview.backend.repository.PracticeMainRepository;
import com.hellointerview.backend.repository.PracticeRepository;
import com.hellointerview.backend.repository.PracticeTranscriptSegmentHistoryRepository;
import com.hellointerview.backend.repository.PracticeTranscriptSegmentRepository;
import com.hellointerview.backend.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PracticeMainServiceTest {

    private PracticeMainRepository practiceMainRepository;
    private PracticeRepository practiceRepository;
    private PracticeHistoryRepository practiceHistoryRepository;
    private PracticeMainHistoryRepository practiceMainHistoryRepository;
    private PracticeTranscriptSegmentRepository practiceTranscriptSegmentRepository;
    private PracticeTranscriptSegmentHistoryRepository practiceTranscriptSegmentHistoryRepository;
    private QuestionRepository questionRepository;
    private PracticeFeedbackRepository practiceFeedbackRepository;
    private PracticeFeedbackHistoryRepository practiceFeedbackHistoryRepository;

    private PracticeMainService practiceMainService;

    @BeforeEach
    void setUp() {
        practiceMainRepository = mock(PracticeMainRepository.class);
        practiceRepository = mock(PracticeRepository.class);
        practiceHistoryRepository = mock(PracticeHistoryRepository.class);
        practiceMainHistoryRepository = mock(PracticeMainHistoryRepository.class);
        practiceTranscriptSegmentRepository = mock(PracticeTranscriptSegmentRepository.class);
        practiceTranscriptSegmentHistoryRepository = mock(PracticeTranscriptSegmentHistoryRepository.class);
        questionRepository = mock(QuestionRepository.class);
        practiceFeedbackRepository = mock(PracticeFeedbackRepository.class);
        practiceFeedbackHistoryRepository = mock(PracticeFeedbackHistoryRepository.class);

        when(practiceFeedbackRepository.findByPractice_PracticeIdIn(anyList())).thenReturn(Collections.emptyList());

        practiceMainService = new PracticeMainService(
                practiceMainRepository,
                practiceRepository,
                practiceHistoryRepository,
                practiceMainHistoryRepository,
                practiceTranscriptSegmentRepository,
                practiceTranscriptSegmentHistoryRepository,
                questionRepository,
                practiceFeedbackRepository,
                practiceFeedbackHistoryRepository
        );
    }

    @Test
    void getPracticeQuestionState_WhenFound_ReturnsAggregatedDto() {
        PracticeMain practiceMain = new PracticeMain();
        practiceMain.setPracticeMainId(123L);
        Question question = new Question();
        question.setQuestionId(456L);

        Practice practice = new Practice();
        practice.setPracticeId(789L);
        practice.setPracticeMain(practiceMain);
        practice.setQuestion(question);

        PracticeTranscriptSegment segment = PracticeTranscriptSegment.builder()
                .segmentOrder(1)
                .transcriptText("hello world")
                .durationSeconds(12)
                .build();

        when(practiceRepository.findByPracticeMain_PracticeMainIdAndQuestion_QuestionId(123L, 456L))
                .thenReturn(Optional.of(practice));
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L))
                .thenReturn(List.of(segment));

        PracticeQuestionStateDto dto = practiceMainService.getPracticeQuestionState(123L, 456L);

        assertEquals(789L, dto.getPracticeId());
        assertEquals(123L, dto.getPracticeMainId());
        assertEquals(456L, dto.getQuestionId());
        assertEquals(12, dto.getTotalDurationSeconds());
        assertEquals("hello world", dto.getCombinedTranscript());
        assertEquals(1, dto.getTranscriptSegments().size());
    }

    @Test
    void createOrGetPractice_WhenExisting_ReturnsExistingResult() {
        PracticeMain practiceMain = new PracticeMain();
        practiceMain.setPracticeMainId(123L);
        Question question = new Question();
        question.setQuestionId(456L);
        Practice existingPractice = new Practice();
        existingPractice.setPracticeId(99L);
        existingPractice.setPracticeMain(practiceMain);
        existingPractice.setQuestion(question);

        when(practiceRepository.findByPracticeMain_PracticeMainIdAndQuestion_QuestionId(123L, 456L))
                .thenReturn(Optional.of(existingPractice));
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(99L))
                .thenReturn(List.of());
        when(practiceMainRepository.findById(123L)).thenReturn(Optional.of(practiceMain));
        when(questionRepository.findById(456L)).thenReturn(Optional.of(question));

        PracticeMainService.CreateOrGetPracticeResult result = practiceMainService.createOrGetPractice(123L, 456L);

        assertEquals(false, result.created());
        assertEquals(99L, result.practiceQuestionState().getPracticeId());
    }

    @Test
    void createOrGetPractice_WhenQuestionIdMissing_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> practiceMainService.createOrGetPractice(123L, null));
    }

    @Test
    void completePracticeSession_WhenActivePracticing_ArchivesAndDeletesAndReturnsCompleted() {
        Long practiceMainId = 123L;
        Instant startedAt = Instant.parse("2026-02-13T09:00:00Z");

        PracticeMain active = new PracticeMain();
        active.setPracticeMainId(practiceMainId);
        active.setUserId(456L);
        active.setQuestionMainId(1L);
        active.setStatus("practicing");
        active.setStartedAt(startedAt);
        Map<String, Object> whiteboard = new LinkedHashMap<>();
        whiteboard.put("section_1", Collections.emptyMap());
        active.setWhiteboardContent(whiteboard);

        Question question = new Question();
        question.setQuestionId(99L);

        Practice practice = new Practice();
        practice.setPracticeId(10L);
        practice.setPracticeMain(active);
        practice.setQuestion(question);
        practice.setSubmittedAt(startedAt.plusSeconds(600));

        when(practiceMainRepository.findById(practiceMainId)).thenReturn(Optional.of(active));
        when(practiceRepository.findByPracticeMain_PracticeMainId(practiceMainId))
                .thenReturn(Collections.singletonList(practice));
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdIn(List.of(10L)))
                .thenReturn(Collections.emptyList());

        PracticeMain completed = practiceMainService.completePracticeSession(practiceMainId);

        assertEquals("completed", completed.getStatus());
        assertEquals(practiceMainId, completed.getPracticeMainId());
        assertEquals(startedAt, completed.getStartedAt());
        assertNotNull(completed.getCompletedAt());
        assertEquals(whiteboard, completed.getWhiteboardContent());

        verify(practiceMainHistoryRepository).save(any(PracticeMainHistory.class));
        verify(practiceHistoryRepository).saveAll(any(List.class));
        verify(practiceFeedbackHistoryRepository, never()).saveAll(anyList());
        verify(practiceMainRepository).delete(eq(active));
    }

    @Test
    void completePracticeSession_WhenPracticeHasFeedback_ArchivesFeedbackToHistory() {
        Long practiceMainId = 123L;
        Instant startedAt = Instant.parse("2026-02-13T09:00:00Z");
        Instant feedbackAt = Instant.parse("2026-02-13T10:00:00Z");

        PracticeMain active = new PracticeMain();
        active.setPracticeMainId(practiceMainId);
        active.setUserId(456L);
        active.setQuestionMainId(1L);
        active.setStatus("practicing");
        active.setStartedAt(startedAt);
        Map<String, Object> whiteboard = new LinkedHashMap<>();
        whiteboard.put("section_1", Collections.emptyMap());
        active.setWhiteboardContent(whiteboard);

        Question question = new Question();
        question.setQuestionId(99L);

        Practice practice = new Practice();
        practice.setPracticeId(10L);
        practice.setPracticeMain(active);
        practice.setQuestion(question);
        practice.setSubmittedAt(startedAt.plusSeconds(600));

        PracticeFeedback feedback = PracticeFeedback.builder()
                .practice(practice)
                .feedbackText("Great work")
                .score(88.0)
                .generatedAt(feedbackAt)
                .build();

        when(practiceMainRepository.findById(practiceMainId)).thenReturn(Optional.of(active));
        when(practiceRepository.findByPracticeMain_PracticeMainId(practiceMainId))
                .thenReturn(Collections.singletonList(practice));
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdIn(List.of(10L)))
                .thenReturn(Collections.emptyList());
        when(practiceFeedbackRepository.findByPractice_PracticeIdIn(List.of(10L)))
                .thenReturn(List.of(feedback));

        practiceMainService.completePracticeSession(practiceMainId);

        verify(practiceFeedbackHistoryRepository).saveAll(argThat((Iterable<PracticeFeedbackHistory> it) -> {
            List<PracticeFeedbackHistory> rows = new ArrayList<>();
            it.forEach(rows::add);
            return rows.size() == 1
                    && "Great work".equals(rows.get(0).getFeedbackText())
                    && Double.valueOf(88.0).equals(rows.get(0).getScore())
                    && feedbackAt.equals(rows.get(0).getGeneratedAt())
                    && Long.valueOf(10L).equals(rows.get(0).getPractice().getPracticeId());
        }));
    }

    @Test
    void updatePracticeMain_WhenWhiteboardContentNull_DoesNotOverrideExisting() {
        Long practiceMainId = 123L;
        Instant startedAt = Instant.parse("2026-02-13T09:00:00Z");

        PracticeMain existing = new PracticeMain();
        existing.setPracticeMainId(practiceMainId);
        existing.setUserId(456L);
        existing.setQuestionMainId(1L);
        existing.setStatus("practicing");
        existing.setStartedAt(startedAt);

        Map<String, Object> originalWhiteboard = new LinkedHashMap<>();
        originalWhiteboard.put("section_1", Collections.singletonMap("type", "diagram"));
        existing.setWhiteboardContent(originalWhiteboard);

        when(practiceMainRepository.findById(practiceMainId)).thenReturn(Optional.of(existing));
        when(practiceMainRepository.save(any(PracticeMain.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PracticeMain updated = practiceMainService.updatePracticeMain(practiceMainId, "practicing", null);

        assertEquals("practicing", updated.getStatus());
        assertEquals(originalWhiteboard, updated.getWhiteboardContent());
    }

    @Test
    void completePracticeSession_WhenAlreadyInHistory_IsIdempotent() {
        Long practiceMainId = 123L;
        Instant startedAt = Instant.parse("2026-02-13T09:00:00Z");
        Instant completedAt = Instant.parse("2026-02-13T11:30:00Z");

        when(practiceMainRepository.findById(practiceMainId)).thenReturn(Optional.empty());

        PracticeMainHistory history = PracticeMainHistory.builder()
                .practiceMainId(practiceMainId)
                .userId(456L)
                .questionMainId(1L)
                .status("completed")
                .startedAt(startedAt)
                .completedAt(completedAt)
                .whiteboardContent(new LinkedHashMap<>())
                .build();

        when(practiceMainHistoryRepository.findById(practiceMainId)).thenReturn(Optional.of(history));

        PracticeMain completed = practiceMainService.completePracticeSession(practiceMainId);

        assertEquals("completed", completed.getStatus());
        assertEquals(completedAt, completed.getCompletedAt());
        assertEquals(practiceMainId, completed.getPracticeMainId());
    }

    @Test
    void completePracticeSession_WhenNotFound_ThrowsResourceNotFound() {
        Long practiceMainId = 123L;
        when(practiceMainRepository.findById(practiceMainId)).thenReturn(Optional.empty());
        when(practiceMainHistoryRepository.findById(practiceMainId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> practiceMainService.completePracticeSession(practiceMainId));
    }
}

