package com.hellointerview.backend.controller;

import com.hellointerview.backend.entity.QuestionMain;
import com.hellointerview.backend.service.QuestionMainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/question-mains")
public class QuestionMainController {

    private final QuestionMainService questionMainService;

    public QuestionMainController(QuestionMainService questionMainService) {
        this.questionMainService = questionMainService;
    }

    /**
     * GET /api/v1/question-mains/{id}
     * Retrieves a QuestionMain by ID with all associated Questions.
     *
     * @param id the QuestionMain ID
     * @return ResponseEntity with QuestionMain data (200 OK) or error (404 Not Found)
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuestionMain> getQuestionMainById(@PathVariable Long id) {
        QuestionMain questionMain = questionMainService.getQuestionMainById(id);
        return ResponseEntity.ok(questionMain);
    }
}
