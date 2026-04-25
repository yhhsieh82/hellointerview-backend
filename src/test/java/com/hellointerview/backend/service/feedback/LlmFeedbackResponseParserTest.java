 package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmFeedbackResponseParserTest {

    private final LlmFeedbackResponseParser parser = new LlmFeedbackResponseParser(new ObjectMapper());

    @Test
    void parse_WhenPayloadValid_ReturnsResult() {
        LlmFeedbackResult result = parser.parse("""
                {"feedback_text":"Good trade-off analysis","score":88}
                """, "Gemini");

        assertEquals("Good trade-off analysis", result.feedbackText());
        assertEquals(88.0, result.score());
    }

    @Test
    void parse_WhenMissingRequiredFields_ThrowsTerminalProviderException() {
        LlmProviderException ex = assertThrows(LlmProviderException.class, () ->
                parser.parse("""
                        {"feedback_text":"","score":"bad"}
                        """, "Gemini")
        );

        assertTrue(!ex.isTransientFailure());
    }
}
