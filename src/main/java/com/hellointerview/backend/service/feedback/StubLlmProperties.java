package com.hellointerview.backend.service.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ai.llm.stub")
public record StubLlmProperties(
        @NotBlank String model,
        @Min(1) @Max(5) int maxAttempts,
        @NotNull Duration initialBackoff,
        double backoffMultiplier,
        @Min(0) @Max(5000) int maxJitterMillis,
        @Min(0) @Max(30000) int latencyDelayMillis
) implements LlmRetryPolicyProperties {
    public StubLlmProperties {
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("ai.llm.stub.backoff-multiplier must be >= 1.0");
        }
    }
}
