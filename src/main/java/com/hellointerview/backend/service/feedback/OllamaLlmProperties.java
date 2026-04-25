package com.hellointerview.backend.service.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "ai.llm.ollama")
public record OllamaLlmProperties(
        @NotBlank String baseUrl,
        @NotBlank String model,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout,
        @Min(1) @Max(5) int maxAttempts,
        @NotNull Duration initialBackoff,
        double backoffMultiplier,
        @Min(0) @Max(5000) int maxJitterMillis
) implements LlmRetryPolicyProperties {
    public OllamaLlmProperties {
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("ai.llm.ollama.backoff-multiplier must be >= 1.0");
        }
    }
}
