package com.hellointerview.backend.controller;

import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.service.PracticeMainService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/practice-main")
public class PracticeMainController {

    private final PracticeMainService practiceMainService;

    public PracticeMainController(PracticeMainService practiceMainService) {
        this.practiceMainService = practiceMainService;
    }

    /**
     * GET /api/v1/practice-main
     * Retrieves the active PracticeMain for a given user and QuestionMain.
     */
    @GetMapping
    public ResponseEntity<PracticeMain> getActivePracticeMain(
            @RequestParam("user_id") Long userId,
            @RequestParam("question_main_id") Long questionMainId,
            @RequestParam(value = "status", defaultValue = "practicing") String status
    ) {
        PracticeMain practiceMain = practiceMainService.getActivePracticeMain(userId, questionMainId, status);
        return ResponseEntity.ok(practiceMain);
    }

    /**
     * POST /api/v1/practice-main
     * Creates a new PracticeMain for a user and QuestionMain.
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
     * Updates the status of an existing PracticeMain.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<PracticeMain> updatePracticeMainStatus(
            @PathVariable("id") Long practiceMainId,
            @RequestBody PracticeMain request
    ) {
        PracticeMain updated = practiceMainService.updatePracticeMainStatus(
                practiceMainId,
                request.getStatus()
        );
        return ResponseEntity.ok(updated);
    }
}

