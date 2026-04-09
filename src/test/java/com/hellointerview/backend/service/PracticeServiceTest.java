package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.PracticeSubmitRequest;
import com.hellointerview.backend.dto.PracticeSubmitResponse;
import com.hellointerview.backend.dto.PracticeTranscriptStateResponse;
import com.hellointerview.backend.dto.TranscriptSegmentSaveRequest;
import com.hellointerview.backend.dto.TranscriptSegmentSaveResponse;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.entity.PracticeTranscriptSegment;
import com.hellointerview.backend.entity.Question;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeRepository;
import com.hellointerview.backend.repository.PracticeTranscriptSegmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PracticeServiceTest {

    private PracticeRepository practiceRepository;
    private PracticeTranscriptSegmentRepository practiceTranscriptSegmentRepository;
    private PracticeService practiceService;

    @BeforeEach
    void setUp() {
        practiceRepository = mock(PracticeRepository.class);
        practiceTranscriptSegmentRepository = mock(PracticeTranscriptSegmentRepository.class);
        practiceService = new PracticeService(practiceRepository, practiceTranscriptSegmentRepository);
    }

    @Test
    void saveTranscriptSegment_WhenValid_ReturnsAggregatedResponse() {
        Practice practice = buildPractice(789L, 333L, 456L, true);
        TranscriptSegmentSaveRequest request = new TranscriptSegmentSaveRequest();
        request.setTranscriptText("I would use stateless API servers");
        request.setDurationSeconds(95);

        PracticeTranscriptSegment segment1 = PracticeTranscriptSegment.builder()
                .segmentOrder(1)
                .transcriptText("First I would define requirements.")
                .durationSeconds(80)
                .build();
        PracticeTranscriptSegment segment2 = PracticeTranscriptSegment.builder()
                .segmentOrder(2)
                .transcriptText("I would use stateless API servers")
                .durationSeconds(95)
                .build();

        when(practiceRepository.findById(789L)).thenReturn(Optional.of(practice));
        when(practiceTranscriptSegmentRepository.findTotalDurationSecondsByPracticeId(789L)).thenReturn(80);
        when(practiceTranscriptSegmentRepository.findMaxSegmentOrderByPracticeId(789L)).thenReturn(1);
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L))
                .thenReturn(List.of(segment1, segment2));

        TranscriptSegmentSaveResponse response = practiceService.saveTranscriptSegment(789L, request);

        assertEquals(789L, response.getPracticeId());
        assertEquals(2, response.getSegmentOrder());
        assertEquals(95, response.getDurationSeconds());
        assertEquals(175, response.getTotalDurationSeconds());
        assertEquals("First I would define requirements. I would use stateless API servers", response.getCombinedTranscript());
        verify(practiceTranscriptSegmentRepository).save(any(PracticeTranscriptSegment.class));
    }

    @Test
    void saveTranscriptSegment_WhenDurationWouldExceedLimit_ThrowsBadRequest() {
        Practice practice = buildPractice(789L, 333L, 456L, true);
        TranscriptSegmentSaveRequest request = new TranscriptSegmentSaveRequest();
        request.setTranscriptText("extra segment");
        request.setDurationSeconds(11);

        when(practiceRepository.findById(789L)).thenReturn(Optional.of(practice));
        when(practiceTranscriptSegmentRepository.findTotalDurationSecondsByPracticeId(789L)).thenReturn(590);

        assertThrows(BadRequestException.class, () -> practiceService.saveTranscriptSegment(789L, request));
    }

    @Test
    void saveTranscriptSegment_WhenTranscriptBlank_ThrowsBadRequest() {
        Practice practice = buildPractice(789L, 333L, 456L, true);
        TranscriptSegmentSaveRequest request = new TranscriptSegmentSaveRequest();
        request.setTranscriptText("  ");
        request.setDurationSeconds(10);

        when(practiceRepository.findById(789L)).thenReturn(Optional.of(practice));

        assertThrows(BadRequestException.class, () -> practiceService.saveTranscriptSegment(789L, request));
    }

    @Test
    void saveTranscriptSegment_WhenPracticeMissing_ThrowsNotFound() {
        TranscriptSegmentSaveRequest request = new TranscriptSegmentSaveRequest();
        request.setTranscriptText("segment");
        request.setDurationSeconds(10);
        when(practiceRepository.findById(789L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> practiceService.saveTranscriptSegment(789L, request));
    }

    @Test
    void getPracticeTranscriptState_WhenValid_ReturnsOrderedSegmentsAndAggregates() {
        Practice practice = buildPractice(789L, 333L, 456L, true);
        PracticeTranscriptSegment segment1 = PracticeTranscriptSegment.builder()
                .segmentOrder(1)
                .transcriptText("First segment")
                .durationSeconds(80)
                .build();
        PracticeTranscriptSegment segment2 = PracticeTranscriptSegment.builder()
                .segmentOrder(2)
                .transcriptText("Second segment")
                .durationSeconds(70)
                .build();

        when(practiceRepository.findById(789L)).thenReturn(Optional.of(practice));
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L))
                .thenReturn(List.of(segment1, segment2));

        PracticeTranscriptStateResponse response = practiceService.getPracticeTranscriptState(789L);

        assertEquals(789L, response.getPracticeId());
        assertEquals(456L, response.getQuestionId());
        assertEquals(2, response.getTranscriptSegments().size());
        assertEquals(150, response.getTotalDurationSeconds());
        assertEquals("First segment Second segment", response.getCombinedTranscript());
    }

    @Test
    void submitPractice_WhenAudioRequiredAndValid_ReturnsResponse() {
        Practice practice = buildPractice(789L, 333L, 456L, true);
        PracticeSubmitRequest request = new PracticeSubmitRequest();
        request.setPracticeId(789L);
        request.setPracticeMainId(333L);
        request.setQuestionId(456L);
        request.setCombinedTranscript("First segment Second segment");
        request.setTotalDurationSeconds(150);

        PracticeTranscriptSegment segment1 = PracticeTranscriptSegment.builder()
                .segmentOrder(1)
                .transcriptText("First segment")
                .durationSeconds(80)
                .build();
        PracticeTranscriptSegment segment2 = PracticeTranscriptSegment.builder()
                .segmentOrder(2)
                .transcriptText("Second segment")
                .durationSeconds(70)
                .build();

        when(practiceRepository.findById(789L)).thenReturn(Optional.of(practice));
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L))
                .thenReturn(List.of(segment1, segment2));

        PracticeSubmitResponse response = practiceService.submitPractice(request);

        assertEquals(789L, response.getPracticeId());
        assertEquals(333L, response.getPracticeMainId());
        assertEquals(456L, response.getQuestionId());
        assertEquals(150, response.getAcceptedTotalDurationSeconds());
    }

    @Test
    void submitPractice_WhenNonAudioAndTranscriptMissing_AllowsSubmission() {
        Practice practice = buildPractice(789L, 333L, 456L, false);
        PracticeSubmitRequest request = new PracticeSubmitRequest();
        request.setPracticeId(789L);
        request.setPracticeMainId(333L);
        request.setQuestionId(456L);

        when(practiceRepository.findById(789L)).thenReturn(Optional.of(practice));
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> practiceService.submitPractice(request));
    }

    @Test
    void submitPractice_WhenAudioRequiredAndTranscriptMissing_ThrowsBadRequest() {
        Practice practice = buildPractice(789L, 333L, 456L, true);
        PracticeSubmitRequest request = new PracticeSubmitRequest();
        request.setPracticeId(789L);
        request.setPracticeMainId(333L);
        request.setQuestionId(456L);

        when(practiceRepository.findById(789L)).thenReturn(Optional.of(practice));
        when(practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(789L))
                .thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> practiceService.submitPractice(request));
    }

    @Test
    void submitPractice_WhenPracticeMainMismatch_ThrowsBadRequest() {
        Practice practice = buildPractice(789L, 333L, 456L, false);
        PracticeSubmitRequest request = new PracticeSubmitRequest();
        request.setPracticeId(789L);
        request.setPracticeMainId(999L);
        request.setQuestionId(456L);

        when(practiceRepository.findById(789L)).thenReturn(Optional.of(practice));

        assertThrows(BadRequestException.class, () -> practiceService.submitPractice(request));
    }

    private static Practice buildPractice(Long practiceId, Long practiceMainId, Long questionId, boolean requiresRecording) {
        Question question = new Question();
        question.setQuestionId(questionId);
        question.setRequiresRecording(requiresRecording);
        PracticeMain practiceMain = new PracticeMain();
        practiceMain.setPracticeMainId(practiceMainId);
        Practice practice = new Practice();
        practice.setPracticeId(practiceId);
        practice.setQuestion(question);
        practice.setPracticeMain(practiceMain);
        return practice;
    }
}
