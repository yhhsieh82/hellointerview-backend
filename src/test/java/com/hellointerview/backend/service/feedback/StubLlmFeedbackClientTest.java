package com.hellointerview.backend.service.feedback;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StubLlmFeedbackClientTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final LabRequestContextResolver contextResolver = new LabRequestContextResolver();
    private final StubLlmProperties properties = new StubLlmProperties(
            "stub-lab-v1",
            2,
            Duration.ZERO,
            1.0,
            0,
            1
    );

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void generate_WhenNoFaultHeaders_ReturnsSuccess() {
        StubLlmFeedbackClient client = new StubLlmFeedbackClient(meterRegistry, properties, contextResolver);
        setRequestHeaders();

        LlmFeedbackResult result = client.generate(input());

        assertTrue(result.feedbackText().contains("Stub AI feedback"));
        assertEquals(85.5, result.score());
    }

    @Test
    void generate_When429FaultConfigured_RetriesThenThrowsTransientProviderException() {
        StubLlmFeedbackClient client = new StubLlmFeedbackClient(meterRegistry, properties, contextResolver);
        setRequestHeaders("A", "429");

        LlmProviderException ex = assertThrows(LlmProviderException.class, () -> client.generate(input()));

        assertTrue(ex.isTransientFailure());
        assertEquals(2.0, totalCounter("llm_provider_calls_total"));
    }

    @Test
    void generate_WhenStrategyB_UsesSingleAttempt() {
        StubLlmFeedbackClient client = new StubLlmFeedbackClient(meterRegistry, properties, contextResolver);
        setRequestHeaders("B", "5xx");

        LlmProviderException ex = assertThrows(LlmProviderException.class, () -> client.generate(input()));

        assertTrue(ex.isTransientFailure());
        assertEquals(1.0, totalCounter("llm_provider_calls_total"));
    }

    private double totalCounter(String meterName) {
        return meterRegistry.find(meterName)
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private void setRequestHeaders() {
        setRequestHeaders("A", "none");
    }

    private void setRequestHeaders(String strategyId, String faultMode) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Lab-Strategy-Id", strategyId);
        request.addHeader("X-Lab-Fault-Mode", faultMode);
        request.addHeader("X-Lab-Fault-Start-Sec", "0");
        request.addHeader("X-Lab-Fault-End-Sec", "300");
        request.addHeader("X-Lab-Test-Start-Epoch-Sec", String.valueOf(System.currentTimeMillis() / 1000L));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static LlmFeedbackInput input() {
        return new LlmFeedbackInput(1L, "system_design", "question", "diagram", "transcript");
    }
}
