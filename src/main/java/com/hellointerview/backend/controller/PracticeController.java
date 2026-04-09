package com.hellointerview.backend.controller;

import com.hellointerview.backend.dto.PracticeTranscriptStateResponse;
import com.hellointerview.backend.dto.PracticeSubmitRequest;
import com.hellointerview.backend.dto.PracticeSubmitResponse;
import com.hellointerview.backend.dto.TranscriptSegmentSaveRequest;
import com.hellointerview.backend.dto.TranscriptSegmentSaveResponse;
import com.hellointerview.backend.service.PracticeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
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
            @RequestBody PracticeSubmitRequest request
    ) {
        PracticeSubmitResponse response = practiceService.submitPractice(request);
        return ResponseEntity.ok(response);
    }
}
