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

class OllamaLlmFeedbackClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generate_WhenResponseValid_ReturnsParsedResult() throws Exception {
        startServer(exchange -> {
            writeResponse(exchange, 200, "{\"response\":\"{\\\"feedback_text\\\":\\\"Looks good\\\",\\\"score\\\":84.5}\"}");
        });

        OllamaClientWithRegistry client = newClient(2);
        LlmFeedbackResult result = client.client().generate(input());

        assertEquals("Looks good", result.feedbackText());
        assertEquals(84.5, result.score());
        assertEquals(1.0, client.registry().get("llm_provider_calls_total")
                .tag("provider", "Ollama")
                .tag("outcome", "success")
                .counter()
                .count());
    }

    @Test
    void generate_WhenTransientHttpFailure_RetriesAndSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        startServer(exchange -> {
            int attempt = calls.incrementAndGet();
            if (attempt == 1) {
                writeResponse(exchange, 500, "{\"error\":\"temporary\"}");
                return;
            }
            writeResponse(exchange, 200, "{\"response\":\"{\\\"feedback_text\\\":\\\"Recovered\\\",\\\"score\\\":75}\"}");
        });

        OllamaClientWithRegistry client = newClient(2);
        LlmFeedbackResult result = client.client().generate(input());

        assertEquals(2, calls.get());
        assertEquals("Recovered", result.feedbackText());
        assertEquals(75.0, result.score());
        assertEquals(1.0, client.registry().get("llm_provider_retry_attempts_total")
                .tag("provider", "Ollama")
                .tag("trigger", "5xx")
                .counter()
                .count());
    }

    @Test
    void generate_WhenTerminalHttpFailure_ThrowsTerminalProviderException() throws Exception {
        startServer(exchange -> writeResponse(exchange, 400, "{\"error\":\"bad request\"}"));
        OllamaClientWithRegistry client = newClient(2);

        LlmProviderException ex = assertThrows(LlmProviderException.class, () -> client.client().generate(input()));
        assertTrue(!ex.isTransientFailure());
    }

    @Test
    void generate_WhenResponsePayloadInvalid_ThrowsTerminalProviderException() throws Exception {
        startServer(exchange -> writeResponse(exchange, 200, "{\"response\":\"{\\\"feedback_text\\\":\\\"\\\",\\\"score\\\":\\\"x\\\"}\"}"));
        OllamaClientWithRegistry client = newClient(2);

        LlmProviderException ex = assertThrows(LlmProviderException.class, () -> client.client().generate(input()));
        assertTrue(!ex.isTransientFailure());
    }

    private OllamaClientWithRegistry newClient(int maxAttempts) {
        OllamaLlmProperties props = new OllamaLlmProperties(
                "http://localhost:" + server.getAddress().getPort(),
                "llama3.1:8b",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                maxAttempts,
                Duration.ofMillis(10),
                1.0,
                0
        );
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OllamaLlmFeedbackClient client = new OllamaLlmFeedbackClient(
                RestClient.builder(),
                new ObjectMapper(),
                props,
                LlmProviderMetrics.fromRegistry(registry)
        );
        return new OllamaClientWithRegistry(client, registry);
    }

    private static LlmFeedbackInput input() {
        return new LlmFeedbackInput(123L, "Functional Requirements", "Design Twitter", "Component: API", "Transcript");
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/generate", exchange -> {
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

    private record OllamaClientWithRegistry(OllamaLlmFeedbackClient client, SimpleMeterRegistry registry) {
    }
}
