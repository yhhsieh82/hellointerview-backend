package com.hellointerview.backend.service.feedback;

public record LlmFeedbackInput(
        long practiceId,
        String questionType,
        String questionDescription,
        String diagramTextDescription,
        String combinedTranscript
) {
}
