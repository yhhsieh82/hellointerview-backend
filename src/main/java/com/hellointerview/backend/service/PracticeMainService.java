package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.PracticeMainResponseDto;
import com.hellointerview.backend.dto.PracticeQuestionStateDto;
import com.hellointerview.backend.entity.Question;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeHistory;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.entity.PracticeMainHistory;
import com.hellointerview.backend.entity.PracticeFeedback;
import com.hellointerview.backend.entity.PracticeFeedbackHistory;
import com.hellointerview.backend.entity.PracticeTranscriptSegment;
import com.hellointerview.backend.entity.PracticeTranscriptSegmentHistory;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeFeedbackHistoryRepository;
import com.hellointerview.backend.repository.PracticeFeedbackRepository;
import com.hellointerview.backend.repository.PracticeHistoryRepository;
import com.hellointerview.backend.repository.PracticeMainHistoryRepository;
import com.hellointerview.backend.repository.PracticeMainRepository;
import com.hellointerview.backend.repository.PracticeRepository;
import com.hellointerview.backend.repository.QuestionRepository;
import com.hellointerview.backend.repository.PracticeTranscriptSegmentHistoryRepository;
import com.hellointerview.backend.repository.PracticeTranscriptSegmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PracticeMainService {

    private static final String DEFAULT_STATUS_PRACTICING = "practicing";

    private final PracticeMainRepository practiceMainRepository;
    private final PracticeRepository practiceRepository;
    private final PracticeHistoryRepository practiceHistoryRepository;
    private final PracticeMainHistoryRepository practiceMainHistoryRepository;
    private final PracticeTranscriptSegmentRepository practiceTranscriptSegmentRepository;
    private final PracticeTranscriptSegmentHistoryRepository practiceTranscriptSegmentHistoryRepository;
    private final QuestionRepository questionRepository;
    private final PracticeFeedbackRepository practiceFeedbackRepository;
    private final PracticeFeedbackHistoryRepository practiceFeedbackHistoryRepository;

    public PracticeMainService(PracticeMainRepository practiceMainRepository,
                               PracticeRepository practiceRepository,
                               PracticeHistoryRepository practiceHistoryRepository,
                               PracticeMainHistoryRepository practiceMainHistoryRepository,
                               PracticeTranscriptSegmentRepository practiceTranscriptSegmentRepository,
                               PracticeTranscriptSegmentHistoryRepository practiceTranscriptSegmentHistoryRepository,
                               QuestionRepository questionRepository,
                               PracticeFeedbackRepository practiceFeedbackRepository,
                               PracticeFeedbackHistoryRepository practiceFeedbackHistoryRepository) {
        this.practiceMainRepository = practiceMainRepository;
        this.practiceRepository = practiceRepository;
        this.practiceHistoryRepository = practiceHistoryRepository;
        this.practiceMainHistoryRepository = practiceMainHistoryRepository;
        this.practiceTranscriptSegmentRepository = practiceTranscriptSegmentRepository;
        this.practiceTranscriptSegmentHistoryRepository = practiceTranscriptSegmentHistoryRepository;
        this.questionRepository = questionRepository;
        this.practiceFeedbackRepository = practiceFeedbackRepository;
        this.practiceFeedbackHistoryRepository = practiceFeedbackHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Long> getQuestionIdsWithFeedback(Long practiceMainId) {
        return practiceFeedbackRepository.findDistinctQuestionIdsWithFeedbackByPracticeMainId(practiceMainId);
    }

    @Transactional(readOnly = true)
    public PracticeMainResponseDto getActivePracticeMainWithProgress(Long userId, Long questionMainId, String status) {
        PracticeMain practiceMain = getActivePracticeMain(userId, questionMainId, status);
        ensureDefaultWhiteboardContent(practiceMain);
        List<Long> questionIdsWithFeedback = getQuestionIdsWithFeedback(practiceMain.getPracticeMainId());
        return toResponseDto(practiceMain, questionIdsWithFeedback);
    }

    private static PracticeMainResponseDto toResponseDto(PracticeMain practiceMain, List<Long> questionIdsWithFeedback) {
        return new PracticeMainResponseDto(
                practiceMain.getPracticeMainId(),
                practiceMain.getUserId(),
                practiceMain.getQuestionMainId(),
                practiceMain.getStatus(),
                practiceMain.getStartedAt(),
                practiceMain.getCompletedAt(),
                questionIdsWithFeedback,
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

    @Transactional(readOnly = true)
    public PracticeQuestionStateDto getPracticeQuestionState(Long practiceMainId, Long questionId) {
        validateQuestionId(questionId);
        Practice practice = practiceRepository
                .findByPracticeMain_PracticeMainIdAndQuestion_QuestionId(practiceMainId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException("No practice found for the given practice_main_id and question_id"));
        return toPracticeQuestionStateDto(practice);
    }

    public CreateOrGetPracticeResult createOrGetPractice(Long practiceMainId, Long questionId) {
        validateQuestionId(questionId);
        PracticeMain practiceMain = findPracticeMainById(practiceMainId);
        Question question = findQuestionById(questionId);

        Practice existing = practiceRepository
                .findByPracticeMain_PracticeMainIdAndQuestion_QuestionId(practiceMainId, questionId)
                .orElse(null);
        if (existing != null) {
            return new CreateOrGetPracticeResult(false, toPracticeQuestionStateDto(existing));
        }

        Practice createdPractice = Practice.builder()
                .practiceMain(practiceMain)
                .question(question)
                .build();

        try {
            Practice saved = practiceRepository.save(createdPractice);
            return new CreateOrGetPracticeResult(true, toPracticeQuestionStateDto(saved));
        } catch (DataIntegrityViolationException ex) {
            Practice loaded = practiceRepository
                    .findByPracticeMain_PracticeMainIdAndQuestion_QuestionId(practiceMainId, questionId)
                    .orElseThrow(() -> ex);
            return new CreateOrGetPracticeResult(false, toPracticeQuestionStateDto(loaded));
        }
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
        archivePracticeFeedbackToHistory(practices, practiceHistories);
        archiveTranscriptSegments(practices, practiceHistories);

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

    private PracticeMain findPracticeMainById(Long practiceMainId) {
        return practiceMainRepository.findById(practiceMainId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PracticeMain with id " + practiceMainId + " does not exist"
                ));
    }

    private Question findQuestionById(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question with id " + questionId + " does not exist"));
    }

    private static void validateQuestionId(Long questionId) {
        if (questionId == null) {
            throw new BadRequestException("question_id is required");
        }
    }

    private PracticeQuestionStateDto toPracticeQuestionStateDto(Practice practice) {
        List<PracticeTranscriptSegment> orderedSegments =
                practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(practice.getPracticeId());
        return TranscriptAggregation.toPracticeQuestionStateDto(practice, orderedSegments);
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

    private void archivePracticeFeedbackToHistory(List<Practice> practices, List<PracticeHistory> practiceHistories) {
        if (practices.isEmpty()) {
            return;
        }
        Map<Long, PracticeHistory> historyByPracticeId = practiceHistories.stream()
                .collect(Collectors.toMap(PracticeHistory::getPracticeId, h -> h, (a, b) -> a));
        List<Long> practiceIds = practices.stream().map(Practice::getPracticeId).toList();
        List<PracticeFeedback> feedbacks = practiceFeedbackRepository.findByPractice_PracticeIdIn(practiceIds);
        if (feedbacks.isEmpty()) {
            return;
        }
        List<PracticeFeedbackHistory> rows = new ArrayList<>();
        for (PracticeFeedback fb : feedbacks) {
            PracticeHistory ph = historyByPracticeId.get(fb.getPractice().getPracticeId());
            if (ph == null) {
                throw new IllegalStateException("Missing PracticeHistory for practice_id " + fb.getPractice().getPracticeId());
            }
            rows.add(PracticeFeedbackHistory.builder()
                    .practice(ph)
                    .feedbackText(fb.getFeedbackText())
                    .score(fb.getScore())
                    .generatedAt(fb.getGeneratedAt())
                    .build());
        }
        practiceFeedbackHistoryRepository.saveAll(rows);
    }

    private void archiveTranscriptSegments(List<Practice> practices, List<PracticeHistory> practiceHistories) {
        if (practices.isEmpty()) {
            return;
        }

        List<Long> practiceIds = practices.stream()
                .map(Practice::getPracticeId)
                .toList();
        List<PracticeTranscriptSegment> transcriptSegments =
                practiceTranscriptSegmentRepository.findByPractice_PracticeIdIn(practiceIds);

        if (transcriptSegments.isEmpty()) {
            return;
        }

        Map<Long, PracticeHistory> historyByPracticeId = practiceHistories.stream()
                .collect(java.util.stream.Collectors.toMap(PracticeHistory::getPracticeId, history -> history));

        List<PracticeTranscriptSegmentHistory> transcriptSegmentHistories = transcriptSegments.stream()
                .map(segment -> PracticeTranscriptSegmentHistory.builder()
                        .segmentId(segment.getSegmentId())
                        .practice(historyByPracticeId.get(segment.getPractice().getPracticeId()))
                        .segmentOrder(segment.getSegmentOrder())
                        .transcriptText(segment.getTranscriptText())
                        .durationSeconds(segment.getDurationSeconds())
                        .createdAt(segment.getCreatedAt())
                        .build())
                .toList();

        practiceTranscriptSegmentHistoryRepository.saveAll(transcriptSegmentHistories);
    }

    public record CreateOrGetPracticeResult(boolean created, PracticeQuestionStateDto practiceQuestionState) {
    }
}

