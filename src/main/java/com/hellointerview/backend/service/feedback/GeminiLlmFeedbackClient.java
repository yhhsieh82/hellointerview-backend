package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellointerview.backend.exception.LlmTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "ai.llm", name = "provider", havingValue = "gemini")
public class GeminiLlmFeedbackClient extends AbstractLlmFeedbackClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiLlmFeedbackClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LlmFeedbackResponseParser responseParser;
    private final GeminiLlmProperties properties;

    public GeminiLlmFeedbackClient(RestClient.Builder restClientBuilder,
                                   ObjectMapper objectMapper,
                                   GeminiLlmProperties properties) {
        super(new FeedbackPromptTemplate(), properties, logger);
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalArgumentException("ai.llm.gemini.api-key must not be blank when provider is gemini");
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.readTimeout().toMillis());
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.responseParser = new LlmFeedbackResponseParser(objectMapper);
        this.properties = properties;
    }

    @Override
    protected String providerName() {
        return "Gemini";
    }

    @Override
    protected LlmFeedbackResult invokeProvider(String prompt) throws LlmTimeoutException {
        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", properties.apiKey())
                            .build(properties.model()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "contents", List.of(
                                    Map.of(
                                            "role", "user",
                                            "parts", List.of(
                                                    Map.of("text", prompt)
                                            )
                                    )
                            ),
                            "generationConfig", Map.of("responseMimeType", "application/json")
                    ))
                    .retrieve()
                    .body(String.class);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw new LlmTimeoutException("Timed out waiting for Gemini response");
            }
            throw e;
        }

        if (responseBody == null || responseBody.isBlank()) {
            throw new LlmProviderException("Gemini returned an empty response payload", true);
        }
        return parseGeminiJson(responseBody);
    }

    private LlmFeedbackResult parseGeminiJson(String rawResponse) {
        try {
            JsonNode responseNode = objectMapper.readTree(rawResponse);
            JsonNode textNode = responseNode.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");
            if (!textNode.isTextual() || textNode.asText().isBlank()) {
                throw new LlmProviderException("Gemini response missing content text", false);
            }

            return responseParser.parse(textNode.asText(), providerName());
        } catch (JsonProcessingException e) {
            throw new LlmProviderException("Unable to parse Gemini JSON response", false, e);
        }
    }

    @Override
    protected Duration parseRetryAfter(RestClientResponseException e) {
        HttpHeaders headers = e.getResponseHeaders();
        if (headers == null) {
            return null;
        }
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds <= 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
        } catch (NumberFormatException ignored) {
            logger.debug("Ignoring non-numeric Retry-After header from Gemini: {}", value);
            return null;
        }
    }
}
