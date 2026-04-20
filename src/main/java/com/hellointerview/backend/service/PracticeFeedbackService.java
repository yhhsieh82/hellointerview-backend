package com.hellointerview.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellointerview.backend.dto.FeedbackSubmitResponseDto;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeFeedback;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.entity.PracticeTranscriptSegment;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.exception.ConflictException;
import com.hellointerview.backend.exception.FeedbackInProgressException;
import com.hellointerview.backend.exception.GradeMappingException;
import com.hellointerview.backend.exception.LlmTimeoutException;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeRepository;
import com.hellointerview.backend.repository.PracticeTranscriptSegmentRepository;
import com.hellointerview.backend.service.feedback.DiagramToTextConverter;
import com.hellointerview.backend.service.feedback.FeedbackClaimResult;
import com.hellointerview.backend.service.feedback.FeedbackIdempotencyCoordinator;
import com.hellointerview.backend.service.feedback.FeedbackInputFingerprint;
import com.hellointerview.backend.service.feedback.FeedbackSubmitResponseMapper;
import com.hellointerview.backend.service.feedback.LlmFeedbackClient;
import com.hellointerview.backend.service.feedback.LlmFeedbackInput;
import com.hellointerview.backend.service.feedback.LlmFeedbackResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PracticeFeedbackService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final PracticeRepository practiceRepository;
    private final PracticeTranscriptSegmentRepository transcriptSegmentRepository;
    private final FeedbackIdempotencyCoordinator idempotencyCoordinator;
    private final LlmFeedbackClient llmFeedbackClient;
    private final ObjectMapper objectMapper;

    public PracticeFeedbackService(PracticeRepository practiceRepository,
                                   PracticeTranscriptSegmentRepository transcriptSegmentRepository,
                                   FeedbackIdempotencyCoordinator idempotencyCoordinator,
                                   LlmFeedbackClient llmFeedbackClient,
                                   ObjectMapper objectMapper) {
        this.practiceRepository = practiceRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.idempotencyCoordinator = idempotencyCoordinator;
        this.llmFeedbackClient = llmFeedbackClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Not {@code @Transactional}: claim, LLM, and finalize use separate transactions in
     * {@link FeedbackIdempotencyCoordinator} so the claim commits before external I/O.
     */
    public FeedbackSubmitResponseDto submitFeedback(Long practiceId, String idempotencyKeyHeader) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKeyHeader);

        Practice practice = practiceRepository.findWithMainAndQuestionById(practiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Practice with id " + practiceId + " does not exist"));

        PracticeMain main = practice.getPracticeMain();
        Long userId = main.getUserId();

        List<PracticeTranscriptSegment> segments =
                transcriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(practiceId);

        validateRecordingIfRequired(practice, segments);

        String sectionKey = "section_" + practice.getQuestion().getWhiteboardSection();
        Map<String, Object> whiteboard = main.getWhiteboardContent();
        Map<String, Object> sectionMap = extractSectionMap(whiteboard, sectionKey);
        validateNonEmptyDiagramSection(sectionMap, sectionKey);

        String sectionJson = writeSectionJson(sectionMap);
        String fingerprint = FeedbackInputFingerprint.compute(practiceId, sectionJson, segments);
        String diagramText = DiagramToTextConverter.diagramToText(sectionMap);
        String combinedTranscript = TranscriptAggregation.buildCombinedTranscript(segments);

        LlmFeedbackInput llmInput = new LlmFeedbackInput(
                practiceId,
                practice.getQuestion().getType().getDisplayName(),
                practice.getQuestion().getDescription(),
                diagramText,
                combinedTranscript
        );

        FeedbackClaimResult claim = idempotencyCoordinator.claimOrInsert(userId, normalizedKey, practice, fingerprint);
        if (claim instanceof FeedbackClaimResult.Replay replay) {
            return replay.dto();
        }
        if (claim instanceof FeedbackClaimResult.Conflict conflict) {
            throw new ConflictException(conflict.message());
        }
        if (claim instanceof FeedbackClaimResult.InProgress) {
            throw new FeedbackInProgressException(
                    "Feedback generation is already in progress for this Idempotency-Key",
                    5
            );
        }
        if (!(claim instanceof FeedbackClaimResult.Proceed proceed)) {
            throw new IllegalStateException("Unexpected claim result: " + claim);
        }

        try {
            LlmFeedbackResult result = llmFeedbackClient.generate(llmInput);
            PracticeFeedback saved = idempotencyCoordinator.finalizeSuccessful(proceed.requestId(), practice, result);
            return FeedbackSubmitResponseMapper.toDto(practice, saved);
        } catch (LlmTimeoutException e) {
            idempotencyCoordinator.markRequestFailed(proceed.requestId(), "llm_timeout");
            throw e;
        } catch (GradeMappingException e) {
            idempotencyCoordinator.markRequestFailed(proceed.requestId(), "grade_mapping_failed");
            throw e;
        }
    }

    private static String normalizeIdempotencyKey(String header) {
        if (header == null) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String trimmed = header.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Idempotency-Key header must not be empty");
        }
        if (trimmed.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key exceeds maximum length of " + MAX_IDEMPOTENCY_KEY_LENGTH);
        }
        return trimmed;
    }

    private String writeSectionJson(Map<String, Object> sectionMap) {
        try {
            return objectMapper.writeValueAsString(sectionMap);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Unable to serialize whiteboard section for fingerprint");
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractSectionMap(Map<String, Object> whiteboard, String sectionKey) {
        if (whiteboard == null) {
            return Map.of();
        }
        Object section = whiteboard.get(sectionKey);
        if (section instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private static void validateNonEmptyDiagramSection(Map<String, Object> sectionMap, String sectionKey) {
        Object elementsObj = sectionMap.get("elements");
        if (!(elementsObj instanceof List<?> elements) || elements.isEmpty()) {
            throw new BadRequestException("Whiteboard content must not be empty for section " + sectionKey);
        }
    }

    private static void validateRecordingIfRequired(Practice practice, List<PracticeTranscriptSegment> segments) {
        if (!Boolean.TRUE.equals(practice.getQuestion().getRequiresRecording())) {
            return;
        }
        if (segments == null || segments.isEmpty()) {
            throw new BadRequestException("Spoken explanation is required for this question before requesting feedback");
        }
    }
}
