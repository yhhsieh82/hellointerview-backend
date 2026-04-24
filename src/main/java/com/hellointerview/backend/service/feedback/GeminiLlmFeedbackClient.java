package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellointerview.backend.exception.LlmTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(prefix = "ai.llm", name = "provider", havingValue = "gemini")
public class GeminiLlmFeedbackClient implements LlmFeedbackClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiLlmFeedbackClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiLlmProperties properties;

    public GeminiLlmFeedbackClient(RestClient.Builder restClientBuilder,
                                   ObjectMapper objectMapper,
                                   GeminiLlmProperties properties) {
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
        this.properties = properties;
    }

    @Override
    public LlmFeedbackResult generate(LlmFeedbackInput input) throws LlmTimeoutException {
        String prompt = buildPrompt(input);
        int attempts = properties.maxAttempts();
        Duration nextBackoff = properties.initialBackoff();
        RuntimeException lastTransientFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return callGemini(prompt);
            } catch (LlmTimeoutException e) {
                if (attempt == attempts) {
                    throw e;
                }
                sleepQuietly(nextBackoff.plusMillis(randomJitterMillis()));
                nextBackoff = multiplyBackoff(nextBackoff);
                lastTransientFailure = e;
            } catch (ResourceAccessException e) {
                if (!isTimeout(e)) {
                    throw new LlmProviderException("Gemini network call failed", true, e);
                }
                if (attempt == attempts) {
                    throw new LlmTimeoutException("Timed out waiting for Gemini response");
                }
                sleepQuietly(nextBackoff.plusMillis(randomJitterMillis()));
                nextBackoff = multiplyBackoff(nextBackoff);
                lastTransientFailure = e;
            } catch (RestClientResponseException e) {
                HttpStatusCode statusCode = e.getStatusCode();
                if (isTransientHttp(statusCode.value())) {
                    if (attempt == attempts) {
                        throw new LlmProviderException("Gemini transient failure after retries", true, e);
                    }
                    Duration retryDelay = parseRetryAfter(e.getResponseHeaders());
                    if (retryDelay != null) {
                        sleepQuietly(retryDelay);
                    } else {
                        sleepQuietly(nextBackoff.plusMillis(randomJitterMillis()));
                        nextBackoff = multiplyBackoff(nextBackoff);
                    }
                    lastTransientFailure = e;
                    continue;
                }
                throw new LlmProviderException(
                        "Gemini terminal failure status " + statusCode.value(),
                        false,
                        e
                );
            }
        }

        if (lastTransientFailure != null) {
            throw new LlmProviderException("Gemini transient failure after retries", true, lastTransientFailure);
        }
        throw new LlmProviderException("Gemini call failed unexpectedly", true);
    }

    private LlmFeedbackResult callGemini(String prompt) throws LlmTimeoutException {
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

            JsonNode structuredNode = objectMapper.readTree(textNode.asText());
            JsonNode feedbackText = structuredNode.get("feedback_text");
            JsonNode score = structuredNode.get("score");
            if (feedbackText == null || feedbackText.asText().isBlank() || score == null || !score.isNumber()) {
                throw new LlmProviderException("Gemini response missing feedback_text or numeric score", false);
            }
            return new LlmFeedbackResult(feedbackText.asText(), score.asDouble());
        } catch (JsonProcessingException e) {
            throw new LlmProviderException("Unable to parse Gemini JSON response", false, e);
        }
    }

    private String buildPrompt(LlmFeedbackInput input) {
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

    private static boolean isTimeout(ResourceAccessException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTransientHttp(int status) {
        return status == 429 || status >= 500;
    }

    private Duration multiplyBackoff(Duration current) {
        long millis = Math.round(current.toMillis() * properties.backoffMultiplier());
        return Duration.ofMillis(Math.max(1, millis));
    }

    private int randomJitterMillis() {
        int max = properties.maxJitterMillis();
        if (max <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(max + 1);
    }

    private Duration parseRetryAfter(HttpHeaders headers) {
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

    private void sleepQuietly(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting to retry Gemini call");
        }
    }
}
