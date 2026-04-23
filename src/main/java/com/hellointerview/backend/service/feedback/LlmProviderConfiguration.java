package com.hellointerview.backend.service.feedback;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OllamaLlmProperties.class)
public class LlmProviderConfiguration {
}
