package com.hellointerview.backend.service.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai.llm.feedback.strategy-b-admission")
public record FeedbackStrategyBAdmissionProperties(
        @Min(1) @Max(10_000) int maxConcurrent,
        @Min(1) @Max(86400) int retryAfterSeconds
) {
}
