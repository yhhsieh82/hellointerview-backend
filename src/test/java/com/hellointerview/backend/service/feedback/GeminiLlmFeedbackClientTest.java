package com.hellointerview.backend.service.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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

        GeminiLlmFeedbackClient client = newClient(2);
        LlmFeedbackResult result = client.generate(input());

        assertEquals("Strong decomposition", result.feedbackText());
        assertEquals(92.0, result.score());
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

        GeminiLlmFeedbackClient client = newClient(2);
        LlmFeedbackResult result = client.generate(input());

        assertEquals(2, calls.get());
        assertEquals("Recovered", result.feedbackText());
        assertEquals(75.0, result.score());
    }

    @Test
    void generate_WhenTerminalHttpFailure_ThrowsTerminalProviderException() throws Exception {
        startServer(exchange -> writeResponse(exchange, 401, "{\"error\":\"unauthorized\"}"));
        GeminiLlmFeedbackClient client = newClient(2);

        LlmProviderException ex = assertThrows(LlmProviderException.class, () -> client.generate(input()));
        assertTrue(!ex.isTransientFailure());
    }

    @Test
    void generate_WhenResponsePayloadInvalid_ThrowsTerminalProviderException() throws Exception {
        startServer(exchange -> writeResponse(exchange, 200, """
                {"candidates":[{"content":{"parts":[{"text":"not-json"}]}}]}
                """));
        GeminiLlmFeedbackClient client = newClient(2);

        LlmProviderException ex = assertThrows(LlmProviderException.class, () -> client.generate(input()));
        assertTrue(!ex.isTransientFailure());
    }

    private GeminiLlmFeedbackClient newClient(int maxAttempts) {
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
        return new GeminiLlmFeedbackClient(RestClient.builder(), new ObjectMapper(), props);
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
}
