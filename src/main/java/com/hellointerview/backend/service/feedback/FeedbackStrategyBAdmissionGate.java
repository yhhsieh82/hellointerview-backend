package com.hellointerview.backend.service.feedback;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Component
public class FeedbackStrategyBAdmissionGate {

    private final LabRequestContextResolver labRequestContextResolver;
    private final FeedbackStrategyBAdmissionProperties properties;
    private final LlmProviderMetrics metrics;
    private final ConcurrentHashMap<String, Semaphore> semaphoresByWorkloadKey = new ConcurrentHashMap<>();

    public FeedbackStrategyBAdmissionGate(LabRequestContextResolver labRequestContextResolver,
                                          FeedbackStrategyBAdmissionProperties properties,
                                          MeterRegistry meterRegistry) {
        this.labRequestContextResolver = labRequestContextResolver;
        this.properties = properties;
        this.metrics = LlmProviderMetrics.fromRegistry(meterRegistry);
    }

    /**
     * When {@link AdmissionEnterOutcome#mayProceed()} is false, the caller must not invoke the LLM and must not call
     * {@link #leave(AdmissionEnterOutcome)}. Otherwise {@link #leave(AdmissionEnterOutcome)} must run after the LLM
     * completes (success or failure) if {@link AdmissionEnterOutcome#mustReleaseSemaphore()} is true.
     */
    public AdmissionEnterOutcome tryEnter(LlmFeedbackClient client) {
        if (!isStrategyB()) {
            return AdmissionEnterOutcome.bypass();
        }
        AdmissionWorkloadKey key = client.admissionWorkloadKey();
        Semaphore semaphore = semaphoresByWorkloadKey.computeIfAbsent(
                key.compositeKey(),
                k -> new Semaphore(properties.maxConcurrent(), true)
        );
        if (!semaphore.tryAcquire()) {
            metrics.recordFailureClass(key.provider(), key.model(), "local_capacity_reject");
            return AdmissionEnterOutcome.rejected(properties.retryAfterSeconds());
        }
        return AdmissionEnterOutcome.admitted(key);
    }

    public void leave(AdmissionEnterOutcome outcome) {
        if (!outcome.mustReleaseSemaphore()) {
            return;
        }
        Semaphore semaphore = semaphoresByWorkloadKey.get(outcome.semaphoreKey());
        if (semaphore != null) {
            semaphore.release();
        }
    }

    private boolean isStrategyB() {
        String strategyId = labRequestContextResolver.resolve().strategyId();
        if (strategyId == null || strategyId.isBlank()) {
            return false;
        }
        return "B".equalsIgnoreCase(strategyId.trim());
    }
}
