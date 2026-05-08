package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.exception.LlmTimeoutException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Instant;
import java.util.Locale;

@Service
@Primary
@ConditionalOnProperty(prefix = "ai.llm", name = "provider", havingValue = "stub", matchIfMissing = true)
public class StubLlmFeedbackClient extends AbstractLlmFeedbackClient {

    private static final Logger logger = LoggerFactory.getLogger(StubLlmFeedbackClient.class);

    private final StubLlmProperties properties;
    private final LabRequestContextResolver contextResolver;

    public StubLlmFeedbackClient(MeterRegistry meterRegistry,
                                 StubLlmProperties properties,
                                 LabRequestContextResolver contextResolver) {
        super(new FeedbackPromptTemplate(), new StrategyAwareRetryProperties(properties, contextResolver), logger, LlmProviderMetrics.fromRegistry(meterRegistry));
        this.properties = properties;
        this.contextResolver = contextResolver;
    }

    @Override
    protected String providerName() {
        return "Stub";
    }

    @Override
    protected String modelName() {
        return properties.model();
    }

    @Override
    protected LlmFeedbackResult invokeProvider(String prompt) throws LlmTimeoutException {
        LabRequestContextResolver.LabRequestContext context = contextResolver.resolve();
        FaultMode mode = FaultMode.fromHeader(context.faultMode());
        if (isFaultWindowActive(context)) {
            switch (mode) {
                case THROTTLE_429 -> throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Lab simulated provider throttle");
                case PROVIDER_5XX -> throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Lab simulated provider 5xx");
                case LATENCY -> sleepLatency(properties.latencyDelayMillis());
                case NONE -> {
                    // pass through
                }
            }
        }
        Long practiceId = extractPracticeId(prompt);
        String text = "Stub AI feedback for practice " + practiceId + ". "
                + "Prompt length: " + prompt.length() + " characters.";
        return new LlmFeedbackResult(text, 85.5);
    }

    private boolean isFaultWindowActive(LabRequestContextResolver.LabRequestContext context) {
        if (context.testStartEpochSec() == null || context.faultStartSec() == null || context.faultEndSec() == null) {
            return false;
        }
        long elapsed = Instant.now().getEpochSecond() - context.testStartEpochSec();
        return elapsed >= context.faultStartSec() && elapsed < context.faultEndSec();
    }

    private void sleepLatency(int latencyDelayMillis) {
        if (latencyDelayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while simulating lab latency fault");
        }
    }

    private static Long extractPracticeId(String prompt) {
        String marker = "practice_id=";
        int markerIndex = prompt.indexOf(marker);
        if (markerIndex < 0) {
            return -1L;
        }
        int start = markerIndex + marker.length();
        int end = start;
        while (end < prompt.length() && Character.isDigit(prompt.charAt(end))) {
            end++;
        }
        if (end == start) {
            return -1L;
        }
        try {
            return Long.parseLong(prompt.substring(start, end));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private enum FaultMode {
        NONE,
        THROTTLE_429,
        PROVIDER_5XX,
        LATENCY;

        static FaultMode fromHeader(String value) {
            if (value == null || value.isBlank()) {
                return NONE;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "429" -> THROTTLE_429;
                case "5xx" -> PROVIDER_5XX;
                case "latency" -> LATENCY;
                default -> NONE;
            };
        }
    }

    private record StrategyAwareRetryProperties(
            StubLlmProperties baseProperties,
            LabRequestContextResolver contextResolver
    ) implements LlmRetryPolicyProperties {

        @Override
        public int maxAttempts() {
            String strategy = strategyId();
            return switch (strategy) {
                case "B", "C" -> 1;
                default -> baseProperties.maxAttempts();
            };
        }

        @Override
        public java.time.Duration initialBackoff() {
            String strategy = strategyId();
            if ("C".equals(strategy)) {
                return baseProperties.initialBackoff().multipliedBy(2);
            }
            return baseProperties.initialBackoff();
        }

        @Override
        public double backoffMultiplier() {
            String strategy = strategyId();
            if ("C".equals(strategy)) {
                return Math.max(baseProperties.backoffMultiplier(), 3.0);
            }
            return baseProperties.backoffMultiplier();
        }

        @Override
        public int maxJitterMillis() {
            String strategy = strategyId();
            if ("C".equals(strategy)) {
                return Math.max(baseProperties.maxJitterMillis(), 500);
            }
            return baseProperties.maxJitterMillis();
        }

        private String strategyId() {
            String strategyId = contextResolver.resolve().strategyId();
            if (strategyId == null || strategyId.isBlank()) {
                return "A";
            }
            return strategyId.trim().toUpperCase(Locale.ROOT);
        }
    }
}
