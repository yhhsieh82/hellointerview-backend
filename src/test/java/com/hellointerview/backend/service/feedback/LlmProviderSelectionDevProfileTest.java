package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(
        classes = LlmProviderSelectionDevProfileTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "ai.llm.gemini.api-key=test-key",
                "ai.llm.gemini.base-url=http://localhost"
        }
)
@ActiveProfiles("dev")
class LlmProviderSelectionDevProfileTest {

    @Autowired
    private LlmFeedbackClient llmFeedbackClient;

    @Test
    void devProfile_UsesGeminiClient() {
        assertInstanceOf(GeminiLlmFeedbackClient.class, llmFeedbackClient);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class
    })
    @Import({
            LlmProviderConfiguration.class,
            StubLlmFeedbackClient.class,
            OllamaLlmFeedbackClient.class,
            GeminiLlmFeedbackClient.class
    })
    static class TestApplication {
        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
