package com.hellointerview.backend.controller;

import com.hellointerview.backend.dto.PracticeMainResponseDto;
import com.hellointerview.backend.dto.PracticeMainUpdateRequest;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.service.PracticeMainService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/practice-main")
public class PracticeMainController {

    private final PracticeMainService practiceMainService;

    public PracticeMainController(PracticeMainService practiceMainService) {
        this.practiceMainService = practiceMainService;
    }

    /**
     * GET /api/v1/practice-main
     * Retrieves the active PracticeMain for a given user and QuestionMain, including question_ids_with_feedback
     * and the canonical whiteboard_content.
     */
    @GetMapping
    public ResponseEntity<PracticeMainResponseDto> getActivePracticeMain(
            @RequestParam("user_id") Long userId,
            @RequestParam("question_main_id") Long questionMainId,
            @RequestParam(value = "status", defaultValue = "practicing") String status
    ) {
        PracticeMainResponseDto dto = practiceMainService.getActivePracticeMainWithProgress(userId, questionMainId, status);
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/v1/practice-main
     * Creates a new PracticeMain for a user and QuestionMain, initializing whiteboard_content.
     */
    @PostMapping
    public ResponseEntity<PracticeMain> createPracticeMain(@RequestBody PracticeMain request) {
        PracticeMain created = practiceMainService.createPracticeMain(
                request.getUserId(),
                request.getQuestionMainId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PATCH /api/v1/practice-main/{id}
     * Updates an existing PracticeMain, including status and/or whiteboard_content.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PracticeMain> updatePracticeMain(
            @PathVariable("id") Long practiceMainId,
            @RequestBody PracticeMainUpdateRequest request
    ) {
        PracticeMain updated = practiceMainService.updatePracticeMain(
                practiceMainId,
                request.getStatus(),
                request.getWhiteboardContent()
        );
        return ResponseEntity.ok(updated);
    }
}

