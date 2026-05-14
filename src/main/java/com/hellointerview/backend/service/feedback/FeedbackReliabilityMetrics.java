package com.hellointerview.backend.service.feedback;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

public final class FeedbackReliabilityMetrics {

    private static final String METRIC_REQUESTS_TOTAL = "feedback_requests_total";
    private static final String METRIC_STAGE_LATENCY_MS = "feedback_stage_latency_ms";
    private static final String METRIC_E2E_LATENCY_MS = "feedback_e2e_completion_latency_ms";
    private static final String METRIC_PROVIDER_CALLS_PER_SUCCESS = "feedback_provider_calls_per_success";

    private static final String HEADER_STRATEGY_ID = "X-Lab-Strategy-Id";
    private static final String HEADER_SCENARIO_ID = "X-Lab-Scenario-Id";
    private static final String HEADER_RUN_ID = "X-Lab-Run-Id";

    private static final FeedbackReliabilityMetrics NOOP = new FeedbackReliabilityMetrics(null);

    private final MeterRegistry meterRegistry;

    private FeedbackReliabilityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public static FeedbackReliabilityMetrics fromRegistry(MeterRegistry meterRegistry) {
        return meterRegistry == null ? NOOP : new FeedbackReliabilityMetrics(meterRegistry);
    }

    public static FeedbackReliabilityMetrics noop() {
        return NOOP;
    }

    public void recordRequestOutcome(String outcome) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder(METRIC_REQUESTS_TOTAL)
                .tags(labTags())
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    public void recordStageLatency(String stage, Duration duration) {
        if (meterRegistry == null) {
            return;
        }
        DistributionSummary.builder(METRIC_STAGE_LATENCY_MS)
                .baseUnit("milliseconds")
                .tags(labTags())
                .tag("stage", stage)
                .register(meterRegistry)
                .record(Math.max(0.0, duration.toMillis()));
    }

    public void recordE2eLatency(String outcome, Duration duration) {
        if (meterRegistry == null) {
            return;
        }
        DistributionSummary.builder(METRIC_E2E_LATENCY_MS)
                .baseUnit("milliseconds")
                .tags(labTags())
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(Math.max(0.0, duration.toMillis()));
    }

    public void recordProviderCallsPerSuccess(double providerCallsPerSuccess) {
        if (meterRegistry == null || providerCallsPerSuccess < 0.0) {
            return;
        }
        DistributionSummary.builder(METRIC_PROVIDER_CALLS_PER_SUCCESS)
                .tags(labTags())
                .register(meterRegistry)
                .record(providerCallsPerSuccess);
    }

    private Tags labTags() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return Tags.of("strategy_id", "A", "scenario_id", "unknown", "run_id", "unknown");
        }
        var request = servletAttributes.getRequest();
        if (request == null) {
            return Tags.of("strategy_id", "A", "scenario_id", "unknown", "run_id", "unknown");
        }
        return Tags.of(
                "strategy_id", normalize(request.getHeader(HEADER_STRATEGY_ID), "A"),
                "scenario_id", normalize(request.getHeader(HEADER_SCENARIO_ID), "unknown"),
                "run_id", normalize(request.getHeader(HEADER_RUN_ID), "unknown")
        );
    }

    private static String normalize(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
