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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackStrategyBAdmissionGateTest {

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
    void tryEnter_WhenStrategyA_BypassesWithoutAcquiring() {
        FeedbackStrategyBAdmissionProperties admissionProperties = new FeedbackStrategyBAdmissionProperties(2, 1);
        FeedbackStrategyBAdmissionGate gate = new FeedbackStrategyBAdmissionGate(contextResolver, admissionProperties, meterRegistry);
        StubLlmFeedbackClient client = new StubLlmFeedbackClient(meterRegistry, properties, contextResolver);
        setStrategy("A");

        AdmissionEnterOutcome outcome = gate.tryEnter(client);

        assertTrue(outcome.mayProceed());
        assertFalse(outcome.mustReleaseSemaphore());
        gate.leave(outcome);
    }

    @Test
    void tryEnter_WhenStrategyB_RejectsAfterConcurrentSlotsExhausted() {
        FeedbackStrategyBAdmissionProperties admissionProperties = new FeedbackStrategyBAdmissionProperties(2, 5);
        FeedbackStrategyBAdmissionGate gate = new FeedbackStrategyBAdmissionGate(contextResolver, admissionProperties, meterRegistry);
        StubLlmFeedbackClient client = new StubLlmFeedbackClient(meterRegistry, properties, contextResolver);
        setStrategy("B");

        AdmissionEnterOutcome first = gate.tryEnter(client);
        AdmissionEnterOutcome second = gate.tryEnter(client);
        AdmissionEnterOutcome third = gate.tryEnter(client);

        assertTrue(first.mayProceed());
        assertTrue(second.mayProceed());
        assertFalse(third.mayProceed());
        assertEquals(5, third.retryAfterSecondsIfRejected());

        assertEquals(1.0, failureCounterTotal("local_capacity_reject"));

        gate.leave(first);
        gate.leave(second);

        AdmissionEnterOutcome fourth = gate.tryEnter(client);
        assertTrue(fourth.mayProceed());
        gate.leave(fourth);
    }

    private void setStrategy(String strategyId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Lab-Strategy-Id", strategyId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private double failureCounterTotal(String failureClass) {
        return meterRegistry.find("llm_provider_failures_total")
                .tag("failure_class", failureClass)
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum();
    }
}
