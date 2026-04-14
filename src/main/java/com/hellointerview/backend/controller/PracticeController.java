package com.hellointerview.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellointerview.backend.dto.PracticeTranscriptStateResponse;
import com.hellointerview.backend.dto.PracticeSubmitRequest;
import com.hellointerview.backend.dto.PracticeSubmitResponse;
import com.hellointerview.backend.dto.TranscriptSegmentSaveRequest;
import com.hellointerview.backend.dto.TranscriptSegmentSaveResponse;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.service.PracticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/practice")
public class PracticeController {

    private final PracticeService practiceService;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_SUBMIT_FIELDS = Set.of(
            "practice_id",
            "practice_main_id",
            "question_id",
            "whiteboard_content"
    );

    public PracticeController(PracticeService practiceService, ObjectMapper objectMapper) {
        this.practiceService = practiceService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{practiceId}/transcript-segments")
    public ResponseEntity<TranscriptSegmentSaveResponse> saveTranscriptSegment(
            @PathVariable("practiceId") Long practiceId,
            @RequestBody TranscriptSegmentSaveRequest request
    ) {
        TranscriptSegmentSaveResponse response = practiceService.saveTranscriptSegment(practiceId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{practiceId}")
    public ResponseEntity<PracticeTranscriptStateResponse> getPracticeTranscriptState(
            @PathVariable("practiceId") Long practiceId
    ) {
        PracticeTranscriptStateResponse response = practiceService.getPracticeTranscriptState(practiceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PracticeSubmitResponse> submitPractice(
            @RequestBody Map<String, Object> requestBody
    ) {
        validateNoClientOwnedTranscriptAggregates(requestBody);
        PracticeSubmitRequest request = objectMapper.convertValue(requestBody, PracticeSubmitRequest.class);
        PracticeSubmitResponse response = practiceService.submitPractice(request);
        return ResponseEntity.ok(response);
    }

    private static void validateNoClientOwnedTranscriptAggregates(Map<String, Object> requestBody) {
        for (String field : requestBody.keySet()) {
            if (!ALLOWED_SUBMIT_FIELDS.contains(field)) {
                throw new BadRequestException(
                        "Unsupported field '" + field + "' in request body. Transcript aggregates are backend-owned."
                );
            }
        }
    }
}
