package com.hellointerview.backend.service.feedback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackPromptTemplateTest {

    private final FeedbackPromptTemplate template = new FeedbackPromptTemplate();

    @Test
    void render_WhenInputContainsBlanks_UsesFallbackAndIncludesContract() {
        LlmFeedbackInput input = new LlmFeedbackInput(
                10L,
                "",
                "Design URL shortener",
                null,
                " "
        );

        String prompt = template.render(input);

        assertTrue(prompt.contains("{\"feedback_text\":\"string\",\"score\":number}"));
        assertTrue(prompt.contains("Question Type: (none)"));
        assertTrue(prompt.contains("Question: Design URL shortener"));
        assertTrue(prompt.contains("User Diagram:\n(none)"));
        assertTrue(prompt.contains("Spoken Explanation Transcript (if provided):\n(none)"));
    }
}
