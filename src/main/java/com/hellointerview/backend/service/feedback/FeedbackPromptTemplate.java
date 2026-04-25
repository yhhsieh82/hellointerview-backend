package com.hellointerview.backend.service.feedback;

final class FeedbackPromptTemplate {

    String render(LlmFeedbackInput input) {
        return """
                You are an expert system design interviewer providing constructive feedback.
                Return valid JSON only with this schema:
                {"feedback_text":"string","score":number}

                Question Type: %s
                Question: %s

                User Diagram:
                %s

                Spoken Explanation Transcript (if provided):
                %s

                Evaluate:
                1. Completeness
                2. Correctness
                3. Clarity
                4. Best Practices
                5. Improvements

                Constraints:
                - score must be in range 0-100
                - feedback_text must be specific and actionable
                - output JSON only, no markdown fences
                """.formatted(
                safe(input.questionType()),
                safe(input.questionDescription()),
                safe(input.diagramTextDescription()),
                safe(input.combinedTranscript())
        );
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "(none)" : value;
    }
}
