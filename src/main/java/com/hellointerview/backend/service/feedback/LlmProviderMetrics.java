package com.hellointerview.backend.service.feedback;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class LlmProviderMetrics {

    private static final String METRIC_CALLS_TOTAL = "llm_provider_calls_total";
    private static final String METRIC_CALL_LATENCY_MS = "llm_provider_call_latency_ms";
    private static final String METRIC_FAILURES_TOTAL = "llm_provider_failures_total";
    private static final String METRIC_HTTP_STATUS_TOTAL = "llm_provider_http_status_total";
    private static final String METRIC_RETRY_ATTEMPTS_TOTAL = "llm_provider_retry_attempts_total";
    private static final String METRIC_RETRY_OUTCOME_TOTAL = "llm_provider_retry_outcome_total";
    private static final String METRIC_INFLIGHT_CALLS = "llm_provider_inflight_calls";
    private static final String METRIC_RETRY_AFTER_SECONDS = "llm_provider_retry_after_seconds";

    private static final LlmProviderMetrics NOOP = new LlmProviderMetrics(null);

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicInteger> inflightByProviderModel = new ConcurrentHashMap<>();

    private LlmProviderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    static LlmProviderMetrics fromRegistry(MeterRegistry meterRegistry) {
        return meterRegistry == null ? NOOP : new LlmProviderMetrics(meterRegistry);
    }

    static LlmProviderMetrics noop() {
        return NOOP;
    }

    void incrementInflight(String provider, String model) {
        if (meterRegistry == null) {
            return;
        }
        AtomicInteger gauge = inflightByProviderModel.computeIfAbsent(provider + "::" + model, key -> {
            AtomicInteger inflight = new AtomicInteger(0);
            meterRegistry.gauge(METRIC_INFLIGHT_CALLS, Tags.of("provider", provider, "model", model), inflight);
            return inflight;
        });
        gauge.incrementAndGet();
    }

    void decrementInflight(String provider, String model) {
        if (meterRegistry == null) {
            return;
        }
        AtomicInteger gauge = inflightByProviderModel.get(provider + "::" + model);
        if (gauge == null) {
            return;
        }
        gauge.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }

    void recordCall(String provider, String model, int attempt, String outcome, Duration duration) {
        if (meterRegistry == null) {
            return;
        }
        Tags tags = Tags.of(
                "provider", provider,
                "model", model,
                "attempt", String.valueOf(attempt),
                "outcome", outcome
        );
        Counter.builder(METRIC_CALLS_TOTAL).tags(tags).register(meterRegistry).increment();
        DistributionSummary.builder(METRIC_CALL_LATENCY_MS)
                .baseUnit("milliseconds")
                .tags(tags)
                .register(meterRegistry)
                .record(Math.max(0.0, duration.toMillis()));
    }

    void recordFailureClass(String provider, String model, String failureClass) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder(METRIC_FAILURES_TOTAL)
                .tags("provider", provider, "model", model, "failure_class", failureClass)
                .register(meterRegistry)
                .increment();
    }

    void recordHttpStatus(String provider, String model, int statusCode) {
        if (meterRegistry == null) {
            return;
        }
        String group = (statusCode / 100) + "xx";
        Counter.builder(METRIC_HTTP_STATUS_TOTAL)
                .tags(
                        "provider", provider,
                        "model", model,
                        "http_status_group", group,
                        "status_code", String.valueOf(statusCode)
                )
                .register(meterRegistry)
                .increment();
    }

    void recordRetryAttempt(String provider, String model, String trigger) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder(METRIC_RETRY_ATTEMPTS_TOTAL)
                .tags("provider", provider, "model", model, "trigger", trigger)
                .register(meterRegistry)
                .increment();
    }

    void recordRetryOutcome(String provider, String model, String outcome) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder(METRIC_RETRY_OUTCOME_TOTAL)
                .tags("provider", provider, "model", model, "outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    void recordRetryAfterSeconds(String provider, String model, Duration retryAfter) {
        if (meterRegistry == null || retryAfter == null) {
            return;
        }
        DistributionSummary.builder(METRIC_RETRY_AFTER_SECONDS)
                .baseUnit("seconds")
                .tags("provider", provider, "model", model)
                .register(meterRegistry)
                .record(Math.max(0.0, retryAfter.toMillis() / 1000.0));
    }

    static String classifyHttpStatus(int statusCode) {
        if (statusCode == 429) {
            return "transient_throttle";
        }
        if (statusCode >= 500) {
            return "transient";
        }
        if (statusCode == 400 || statusCode == 422) {
            return "terminal_request";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "terminal_auth_config";
        }
        return "terminal_request";
    }

    static String classifyProviderException(LlmProviderException e) {
        Throwable cause = e.getCause();
        if (cause instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            return "parse";
        }
        String message = e.getMessage();
        if (message != null && message.toLowerCase().contains("parse")) {
            return "parse";
        }
        if (e.isTransientFailure()) {
            return "transient";
        }
        return "terminal_request";
    }
}
