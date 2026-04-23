package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(
        classes = LlmProviderSelectionDefaultProfileTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class LlmProviderSelectionDefaultProfileTest {

    @Autowired
    private LlmFeedbackClient llmFeedbackClient;

    @Test
    void defaultProfile_UsesStubClient() {
        assertInstanceOf(StubLlmFeedbackClient.class, llmFeedbackClient);
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
            OllamaLlmFeedbackClient.class
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
