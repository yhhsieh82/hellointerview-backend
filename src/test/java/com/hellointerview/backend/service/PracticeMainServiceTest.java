package com.hellointerview.backend.service;

import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeHistory;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.entity.PracticeMainHistory;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeHistoryRepository;
import com.hellointerview.backend.repository.PracticeMainHistoryRepository;
import com.hellointerview.backend.repository.PracticeMainRepository;
import com.hellointerview.backend.repository.PracticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PracticeMainServiceTest {

    private PracticeMainRepository practiceMainRepository;
    private PracticeRepository practiceRepository;
    private PracticeHistoryRepository practiceHistoryRepository;
    private PracticeMainHistoryRepository practiceMainHistoryRepository;

    private PracticeMainService practiceMainService;

    @BeforeEach
    void setUp() {
        practiceMainRepository = mock(PracticeMainRepository.class);
        practiceRepository = mock(PracticeRepository.class);
        practiceHistoryRepository = mock(PracticeHistoryRepository.class);
        practiceMainHistoryRepository = mock(PracticeMainHistoryRepository.class);

        practiceMainService = new PracticeMainService(
                practiceMainRepository,
                practiceRepository,
                practiceHistoryRepository,
                practiceMainHistoryRepository
        );
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

        Practice practice = new Practice();
        practice.setPracticeId(10L);
        practice.setPracticeMain(active);
        practice.setSubmittedAt(startedAt.plusSeconds(600));

        when(practiceMainRepository.findById(practiceMainId)).thenReturn(Optional.of(active));
        when(practiceRepository.findByPracticeMain_PracticeMainId(practiceMainId))
                .thenReturn(Collections.singletonList(practice));

        PracticeMain completed = practiceMainService.completePracticeSession(practiceMainId);

        assertEquals("completed", completed.getStatus());
        assertEquals(practiceMainId, completed.getPracticeMainId());
        assertEquals(startedAt, completed.getStartedAt());
        assertNotNull(completed.getCompletedAt());

        verify(practiceMainHistoryRepository).save(any(PracticeMainHistory.class));
        verify(practiceHistoryRepository).saveAll(any(List.class));
        verify(practiceMainRepository).delete(eq(active));
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

