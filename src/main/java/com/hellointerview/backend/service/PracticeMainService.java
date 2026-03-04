package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.PracticeMainResponseDto;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeHistory;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.entity.PracticeMainHistory;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeHistoryRepository;
import com.hellointerview.backend.repository.PracticeMainHistoryRepository;
import com.hellointerview.backend.repository.PracticeMainRepository;
import com.hellointerview.backend.repository.PracticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PracticeMainService {

    private static final String DEFAULT_STATUS_PRACTICING = "practicing";

    private final PracticeMainRepository practiceMainRepository;
    private final PracticeRepository practiceRepository;
    private final PracticeHistoryRepository practiceHistoryRepository;
    private final PracticeMainHistoryRepository practiceMainHistoryRepository;

    public PracticeMainService(PracticeMainRepository practiceMainRepository,
                               PracticeRepository practiceRepository,
                               PracticeHistoryRepository practiceHistoryRepository,
                               PracticeMainHistoryRepository practiceMainHistoryRepository) {
        this.practiceMainRepository = practiceMainRepository;
        this.practiceRepository = practiceRepository;
        this.practiceHistoryRepository = practiceHistoryRepository;
        this.practiceMainHistoryRepository = practiceMainHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Long> getQuestionIdsWithPractices(Long practiceMainId) {
        return practiceRepository.findDistinctQuestionIdsByPracticeMainId(practiceMainId);
    }

    @Transactional(readOnly = true)
    public PracticeMainResponseDto getActivePracticeMainWithProgress(Long userId, Long questionMainId, String status) {
        PracticeMain practiceMain = getActivePracticeMain(userId, questionMainId, status);
        ensureDefaultWhiteboardContent(practiceMain);
        List<Long> questionIdsWithPractices = getQuestionIdsWithPractices(practiceMain.getPracticeMainId());
        return toResponseDto(practiceMain, questionIdsWithPractices);
    }

    private static PracticeMainResponseDto toResponseDto(PracticeMain practiceMain, List<Long> questionIdsWithPractices) {
        return new PracticeMainResponseDto(
                practiceMain.getPracticeMainId(),
                practiceMain.getUserId(),
                practiceMain.getQuestionMainId(),
                practiceMain.getStatus(),
                practiceMain.getStartedAt(),
                practiceMain.getCompletedAt(),
                questionIdsWithPractices,
                practiceMain.getWhiteboardContent()
        );
    }

    @Transactional(readOnly = true)
    public PracticeMain getActivePracticeMain(Long userId, Long questionMainId, String status) {
        String effectiveStatus = (status == null || status.isBlank()) ? DEFAULT_STATUS_PRACTICING : status;

        return practiceMainRepository
                .findByUserIdAndQuestionMainIdAndStatus(userId, questionMainId, effectiveStatus)
                .orElseThrow(() -> new ResourceNotFoundException("No active practice session found"));
    }

    public PracticeMain createPracticeMain(Long userId, Long questionMainId) {
        PracticeMain practiceMain = new PracticeMain();
        practiceMain.setUserId(userId);
        practiceMain.setQuestionMainId(questionMainId);
        practiceMain.setStatus(DEFAULT_STATUS_PRACTICING);
        practiceMain.setWhiteboardContent(createDefaultWhiteboardContent());

        return practiceMainRepository.save(practiceMain);
    }

    public PracticeMain updatePracticeMain(Long practiceMainId, String status, Map<String, Object> whiteboardContent) {
        if ("completed".equals(status)) {
            if (whiteboardContent != null) {
                PracticeMain practiceMain = practiceMainRepository.findById(practiceMainId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "PracticeMain with id " + practiceMainId + " does not exist"
                        ));
                practiceMain.setWhiteboardContent(whiteboardContent);
                practiceMainRepository.save(practiceMain);
            }
            return completePracticeSession(practiceMainId);
        }

        PracticeMain practiceMain = practiceMainRepository.findById(practiceMainId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PracticeMain with id " + practiceMainId + " does not exist"
                ));

        if (whiteboardContent != null) {
            practiceMain.setWhiteboardContent(whiteboardContent);
        }

        if (status != null && !status.isBlank()) {
            practiceMain.setStatus(status);
            practiceMain.setCompletedAt(null);
        }

        return practiceMainRepository.save(practiceMain);
    }

    /**
     * Completes a practice session:
     * - For an active practicing session: archive to history tables and delete from active tables.
     * - For an already archived/completed session: return completed metadata (idempotent).
     */
    public PracticeMain completePracticeSession(Long practiceMainId) {
        return practiceMainRepository.findById(practiceMainId)
                .map(this::archiveAndDeleteActivePracticeMain)
                .orElseGet(() -> loadCompletedFromHistory(practiceMainId));
    }

    private PracticeMain archiveAndDeleteActivePracticeMain(PracticeMain practiceMain) {
        if (!DEFAULT_STATUS_PRACTICING.equals(practiceMain.getStatus())) {
            if ("completed".equals(practiceMain.getStatus())) {
                return practiceMain;
            }
            throw new IllegalStateException("Cannot complete practice session with status: " + practiceMain.getStatus());
        }

        Instant completedAt = Instant.now();

        PracticeMainHistory practiceMainHistory = PracticeMainHistory.builder()
                .practiceMainId(practiceMain.getPracticeMainId())
                .userId(practiceMain.getUserId())
                .questionMainId(practiceMain.getQuestionMainId())
                .status("completed")
                .startedAt(practiceMain.getStartedAt())
                .completedAt(completedAt)
                .whiteboardContent(practiceMain.getWhiteboardContent())
                .build();

        practiceMainHistoryRepository.save(practiceMainHistory);

        List<Practice> practices = practiceRepository.findByPracticeMain_PracticeMainId(practiceMain.getPracticeMainId());

        List<PracticeHistory> practiceHistories = practices.stream()
                .map(practice -> PracticeHistory.builder()
                        .practiceId(practice.getPracticeId())
                        .practiceMainHistory(practiceMainHistory)
                        .question(practice.getQuestion())
                        .submittedAt(practice.getSubmittedAt())
                        .build())
                .toList();

        practiceHistoryRepository.saveAll(practiceHistories);

        practiceMainRepository.delete(practiceMain);

        PracticeMain completed = new PracticeMain();
        completed.setPracticeMainId(practiceMain.getPracticeMainId());
        completed.setUserId(practiceMain.getUserId());
        completed.setQuestionMainId(practiceMain.getQuestionMainId());
        completed.setStatus("completed");
        completed.setStartedAt(practiceMain.getStartedAt());
        completed.setCompletedAt(completedAt);
        completed.setWhiteboardContent(practiceMain.getWhiteboardContent());

        return completed;
    }

    private PracticeMain loadCompletedFromHistory(Long practiceMainId) {
        PracticeMainHistory history = practiceMainHistoryRepository.findById(practiceMainId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PracticeMain with id " + practiceMainId + " does not exist"
                ));

        PracticeMain completed = new PracticeMain();
        completed.setPracticeMainId(history.getPracticeMainId());
        completed.setUserId(history.getUserId());
        completed.setQuestionMainId(history.getQuestionMainId());
        completed.setStatus(history.getStatus());
        completed.setStartedAt(history.getStartedAt());
        completed.setCompletedAt(history.getCompletedAt());
        completed.setWhiteboardContent(history.getWhiteboardContent());

        return completed;
    }

    private void ensureDefaultWhiteboardContent(PracticeMain practiceMain) {
        if (practiceMain.getWhiteboardContent() == null) {
            practiceMain.setWhiteboardContent(createDefaultWhiteboardContent());
            practiceMainRepository.save(practiceMain);
        }
    }

    private static Map<String, Object> createDefaultWhiteboardContent() {
        Map<String, Object> root = new LinkedHashMap<>();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> section = new LinkedHashMap<>();
            section.put("type", "diagram");
            section.put("version", "1.0");
            section.put("elements", new ArrayList<>());
            section.put("appState", new LinkedHashMap<String, Object>());
            section.put("files", new LinkedHashMap<String, Object>());
            root.put("section_" + i, section);
        }

        return root;
    }
}

