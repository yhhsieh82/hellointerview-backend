package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class LlmFeedbackResponseParser {

    private final ObjectMapper objectMapper;

    LlmFeedbackResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    LlmFeedbackResult parse(String responseContent, String providerName) {
        try {
            JsonNode node = objectMapper.readTree(responseContent);
            JsonNode feedbackText = node.get("feedback_text");
            JsonNode score = node.get("score");
            if (feedbackText == null || feedbackText.asText().isBlank() || score == null || !score.isNumber()) {
                throw new LlmProviderException(providerName + " response missing feedback_text or numeric score", false);
            }
            return new LlmFeedbackResult(feedbackText.asText(), score.asDouble());
        } catch (JsonProcessingException e) {
            throw new LlmProviderException("Unable to parse " + providerName + " JSON response", false, e);
        }
    }
}
