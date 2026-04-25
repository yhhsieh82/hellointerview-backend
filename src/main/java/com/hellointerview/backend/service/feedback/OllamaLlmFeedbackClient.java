package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellointerview.backend.exception.LlmTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "ai.llm", name = "provider", havingValue = "ollama")
public class OllamaLlmFeedbackClient extends AbstractLlmFeedbackClient {

    private static final Logger logger = LoggerFactory.getLogger(OllamaLlmFeedbackClient.class);
    private static final String GENERATE_PATH = "/api/generate";

    private final RestClient restClient;
    private final LlmFeedbackResponseParser responseParser;
    private final OllamaLlmProperties properties;

    public OllamaLlmFeedbackClient(RestClient.Builder restClientBuilder,
                                   ObjectMapper objectMapper,
                                   OllamaLlmProperties properties) {
        super(new FeedbackPromptTemplate(), properties, logger);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.readTimeout().toMillis());
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        this.responseParser = new LlmFeedbackResponseParser(objectMapper);
        this.properties = properties;
    }

    @Override
    protected String providerName() {
        return "Ollama";
    }

    @Override
    protected LlmFeedbackResult invokeProvider(String prompt) throws LlmTimeoutException {
        OllamaGenerateResponse response;
        try {
            response = restClient.post()
                    .uri(GENERATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", properties.model(),
                            "prompt", prompt,
                            "stream", false,
                            "format", "json"
                    ))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw new LlmTimeoutException("Timed out waiting for Ollama response");
            }
            throw e;
        }

        if (response == null || response.response() == null || response.response().isBlank()) {
            throw new LlmProviderException("Ollama returned an empty response payload", true);
        }
        return parseModelJson(response.response());
    }

    private LlmFeedbackResult parseModelJson(String responseContent) {
        return responseParser.parse(responseContent, providerName());
    }

    private record OllamaGenerateResponse(String response) {
    }
}
