package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiLlmFeedbackClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generate_WhenResponseValid_ReturnsParsedResult() throws Exception {
        startServer(exchange -> writeResponse(exchange, 200, """
                {"candidates":[{"content":{"parts":[{"text":"{\\"feedback_text\\":\\"Strong decomposition\\",\\"score\\":92}"}]}}]}
                """));

        GeminiClientWithRegistry client = newClient(2);
        LlmFeedbackResult result = client.client().generate(input());

        assertEquals("Strong decomposition", result.feedbackText());
        assertEquals(92.0, result.score());
        assertEquals(1.0, client.registry().get("llm_provider_calls_total")
                .tag("provider", "Gemini")
                .tag("outcome", "success")
                .counter()
                .count());
    }

    @Test
    void generate_WhenTransientHttpFailure_RetriesAndSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        startServer(exchange -> {
            if (calls.incrementAndGet() == 1) {
                writeResponse(exchange, 500, "{\"error\":\"temporary\"}");
                return;
            }
            writeResponse(exchange, 200, """
                    {"candidates":[{"content":{"parts":[{"text":"{\\"feedback_text\\":\\"Recovered\\",\\"score\\":75}"}]}}]}
                    """);
        });

        GeminiClientWithRegistry client = newClient(2);
        LlmFeedbackResult result = client.client().generate(input());

        assertEquals(2, calls.get());
        assertEquals("Recovered", result.feedbackText());
        assertEquals(75.0, result.score());
        assertEquals(1.0, client.registry().get("llm_provider_retry_attempts_total")
                .tag("provider", "Gemini")
                .tag("trigger", "5xx")
                .counter()
                .count());
    }

    @Test
    void generate_When429WithRetryAfter_RecordsRetryAfterMetric() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        startServer(exchange -> {
            if (calls.incrementAndGet() == 1) {
                exchange.getResponseHeaders().add("Retry-After", "1");
                writeResponse(exchange, 429, "{\"error\":\"too many requests\"}");
                return;
            }
            writeResponse(exchange, 200, """
                    {"candidates":[{"content":{"parts":[{"text":"{\\"feedback_text\\":\\"Recovered\\",\\"score\\":80}"}]}}]}
                    """);
        });

        GeminiClientWithRegistry client = newClient(2);
        client.client().generate(input());

        assertEquals(1.0, client.registry().get("llm_provider_retry_after_seconds")
                .tag("provider", "Gemini")
                .summary()
                .count());
    }

    @Test
    void generate_WhenTerminalHttpFailure_ThrowsTerminalProviderException() throws Exception {
        startServer(exchange -> writeResponse(exchange, 401, "{\"error\":\"unauthorized\"}"));
        GeminiClientWithRegistry client = newClient(2);

        LlmProviderException ex = assertThrows(LlmProviderException.class, () -> client.client().generate(input()));
        assertTrue(!ex.isTransientFailure());
    }

    @Test
    void generate_WhenResponsePayloadInvalid_ThrowsTerminalProviderException() throws Exception {
        startServer(exchange -> writeResponse(exchange, 200, """
                {"candidates":[{"content":{"parts":[{"text":"not-json"}]}}]}
                """));
        GeminiClientWithRegistry client = newClient(2);

        LlmProviderException ex = assertThrows(LlmProviderException.class, () -> client.client().generate(input()));
        assertTrue(!ex.isTransientFailure());
    }

    private GeminiClientWithRegistry newClient(int maxAttempts) {
        GeminiLlmProperties props = new GeminiLlmProperties(
                "http://localhost:" + server.getAddress().getPort(),
                "test-key",
                "gemini-2.0-flash",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                maxAttempts,
                Duration.ofMillis(10),
                1.0,
                0
        );
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        GeminiLlmFeedbackClient client = new GeminiLlmFeedbackClient(
                RestClient.builder(),
                new ObjectMapper(),
                props,
                LlmProviderMetrics.fromRegistry(registry)
        );
        return new GeminiClientWithRegistry(client, registry);
    }

    private static LlmFeedbackInput input() {
        return new LlmFeedbackInput(123L, "Functional Requirements", "Design Twitter", "Component: API", "Transcript");
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private record GeminiClientWithRegistry(GeminiLlmFeedbackClient client, SimpleMeterRegistry registry) {
    }
}
