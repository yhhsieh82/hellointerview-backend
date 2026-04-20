package com.hellointerview.backend.controller;

import com.hellointerview.backend.dto.FeedbackSubmitResponseDto;
import com.hellointerview.backend.exception.BadRequestException;
import com.hellointerview.backend.service.PracticeFeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/practices")
public class PracticesController {

    private final PracticeFeedbackService practiceFeedbackService;

    public PracticesController(PracticeFeedbackService practiceFeedbackService) {
        this.practiceFeedbackService = practiceFeedbackService;
    }

    @PostMapping("/{practiceId}/feedbacks")
    public ResponseEntity<FeedbackSubmitResponseDto> submitFeedback(
            @PathVariable("practiceId") Long practiceId,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        if (body != null && !body.isEmpty()) {
            throw new BadRequestException("Request body must be empty for this endpoint");
        }
        FeedbackSubmitResponseDto dto = practiceFeedbackService.submitFeedback(practiceId, idempotencyKey);
        return ResponseEntity.ok(dto);
    }
}
