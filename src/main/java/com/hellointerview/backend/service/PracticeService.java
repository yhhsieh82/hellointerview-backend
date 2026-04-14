package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.PracticeTranscriptStateResponse;
import com.hellointerview.backend.dto.PracticeSubmitRequest;
import com.hellointerview.backend.dto.PracticeSubmitResponse;
import com.hellointerview.backend.dto.TranscriptSegmentDto;
import com.hellointerview.backend.dto.TranscriptSegmentSaveRequest;
import com.hellointerview.backend.dto.TranscriptSegmentSaveResponse;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeTranscriptSegment;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeRepository;
import com.hellointerview.backend.repository.PracticeTranscriptSegmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PracticeService {

    private static final int MAX_TOTAL_DURATION_SECONDS = 600;

    private final PracticeRepository practiceRepository;
    private final PracticeTranscriptSegmentRepository practiceTranscriptSegmentRepository;

    public PracticeService(PracticeRepository practiceRepository,
                           PracticeTranscriptSegmentRepository practiceTranscriptSegmentRepository) {
        this.practiceRepository = practiceRepository;
        this.practiceTranscriptSegmentRepository = practiceTranscriptSegmentRepository;
    }

    public TranscriptSegmentSaveResponse saveTranscriptSegment(Long practiceId, TranscriptSegmentSaveRequest request) {
        Practice practice = findPracticeById(practiceId);
        validateSaveRequest(request);

        Integer currentTotalDuration = practiceTranscriptSegmentRepository.findTotalDurationSecondsByPracticeId(practiceId);
        int nextTotalDuration = currentTotalDuration + request.getDurationSeconds();
        if (nextTotalDuration > MAX_TOTAL_DURATION_SECONDS) {
            throw new BadRequestException("Total speaking time cannot exceed 600 seconds");
        }

        Integer maxOrder = practiceTranscriptSegmentRepository.findMaxSegmentOrderByPracticeId(practiceId);
        int nextSegmentOrder = maxOrder + 1;

        PracticeTranscriptSegment segment = PracticeTranscriptSegment.builder()
                .practice(practice)
                .segmentOrder(nextSegmentOrder)
                .transcriptText(request.getTranscriptText().trim())
                .durationSeconds(request.getDurationSeconds())
                .build();
        practiceTranscriptSegmentRepository.save(segment);

        List<PracticeTranscriptSegment> orderedSegments =
                practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(practiceId);

        return new TranscriptSegmentSaveResponse(
                practiceId,
                nextSegmentOrder,
                request.getDurationSeconds(),
                nextTotalDuration,
                buildCombinedTranscript(orderedSegments)
        );
    }

    public PracticeSubmitResponse submitPractice(PracticeSubmitRequest request) {
        validateSubmitRequest(request);
        Practice practice = findPracticeById(request.getPracticeId());
        validatePracticeRelationships(practice, request);

        List<PracticeTranscriptSegment> orderedSegments =
                practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(practice.getPracticeId());
        int derivedTotalDurationSeconds = orderedSegments.stream()
                .mapToInt(PracticeTranscriptSegment::getDurationSeconds)
                .sum();
        String derivedCombinedTranscript = buildCombinedTranscript(orderedSegments);

        boolean audioRequired = Boolean.TRUE.equals(practice.getQuestion().getRequiresRecording());

        return new PracticeSubmitResponse(
                practice.getPracticeId(),
                practice.getPracticeMain().getPracticeMainId(),
                practice.getQuestion().getQuestionId(),
                audioRequired,
                derivedCombinedTranscript,
                derivedTotalDurationSeconds
        );
    }

    @Transactional(readOnly = true)
    public PracticeTranscriptStateResponse getPracticeTranscriptState(Long practiceId) {
        Practice practice = findPracticeById(practiceId);
        List<PracticeTranscriptSegment> orderedSegments =
                practiceTranscriptSegmentRepository.findByPractice_PracticeIdOrderBySegmentOrderAsc(practiceId);

        List<TranscriptSegmentDto> transcriptSegments = orderedSegments.stream()
                .map(segment -> new TranscriptSegmentDto(
                        segment.getSegmentOrder(),
                        segment.getTranscriptText(),
                        segment.getDurationSeconds()
                ))
                .toList();

        int totalDurationSeconds = orderedSegments.stream()
                .mapToInt(PracticeTranscriptSegment::getDurationSeconds)
                .sum();

        return new PracticeTranscriptStateResponse(
                practice.getPracticeId(),
                practice.getQuestion().getQuestionId(),
                transcriptSegments,
                totalDurationSeconds,
                buildCombinedTranscript(orderedSegments)
        );
    }

    private Practice findPracticeById(Long practiceId) {
        return practiceRepository.findById(practiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Practice with id " + practiceId + " does not exist"));
    }

    private static void validateSaveRequest(TranscriptSegmentSaveRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.getTranscriptText() == null || request.getTranscriptText().isBlank()) {
            throw new BadRequestException("Transcript text cannot be empty");
        }
        if (request.getDurationSeconds() == null || request.getDurationSeconds() <= 0) {
            throw new BadRequestException("Duration seconds must be a positive number");
        }
    }

    private static void validateSubmitRequest(PracticeSubmitRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.getPracticeId() == null) {
            throw new BadRequestException("practice_id is required");
        }
        if (request.getPracticeMainId() == null) {
            throw new BadRequestException("practice_main_id is required");
        }
        if (request.getQuestionId() == null) {
            throw new BadRequestException("question_id is required");
        }
    }

    private static void validatePracticeRelationships(Practice practice, PracticeSubmitRequest request) {
        Long actualPracticeMainId = practice.getPracticeMain().getPracticeMainId();
        if (!actualPracticeMainId.equals(request.getPracticeMainId())) {
            throw new BadRequestException("practice_main_id does not match practice_id");
        }
        Long actualQuestionId = practice.getQuestion().getQuestionId();
        if (!actualQuestionId.equals(request.getQuestionId())) {
            throw new BadRequestException("question_id does not match practice_id");
        }
    }

    private static String buildCombinedTranscript(List<PracticeTranscriptSegment> orderedSegments) {
        return orderedSegments.stream()
                .map(PracticeTranscriptSegment::getTranscriptText)
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}
