package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.exception.LlmTimeoutException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractLlmFeedbackClientTest {

    @Test
    void generate_WhenTransientHttpFailure_RetriesAndSucceeds() throws LlmTimeoutException {
        AtomicInteger calls = new AtomicInteger();
        TestClient client = new TestClient(() -> {
            if (calls.incrementAndGet() == 1) {
                throw new RestClientResponseException("temporary", 500, "Internal Server Error", null, null, null);
            }
            return new LlmFeedbackResult("Recovered", 77);
        });

        LlmFeedbackResult result = client.generate(input());

        assertEquals(2, calls.get());
        assertEquals("Recovered", result.feedbackText());
        assertEquals(77.0, result.score());
    }

    @Test
    void generate_WhenTimeoutResourceAccess_ThrowsLlmTimeoutException() {
        TestClient client = new TestClient(() -> {
            throw new ResourceAccessException("timed out", new SocketTimeoutException("timeout"));
        });

        assertThrows(LlmTimeoutException.class, () -> client.generate(input()));
    }

    private static LlmFeedbackInput input() {
        return new LlmFeedbackInput(1L, "type", "question", "diagram", "transcript");
    }

    private static final class TestClient extends AbstractLlmFeedbackClient {
        private final Invocation invocation;

        private TestClient(Invocation invocation) {
            super(new FeedbackPromptTemplate(), new TestRetryProperties(), LoggerFactory.getLogger(TestClient.class));
            this.invocation = invocation;
        }

        @Override
        protected String providerName() {
            return "TestProvider";
        }

        @Override
        protected LlmFeedbackResult invokeProvider(String prompt) throws LlmTimeoutException {
            return invocation.invoke();
        }
    }

    private record TestRetryProperties() implements LlmRetryPolicyProperties {
        @Override
        public int maxAttempts() {
            return 2;
        }

        @Override
        public Duration initialBackoff() {
            return Duration.ZERO;
        }

        @Override
        public double backoffMultiplier() {
            return 1.0;
        }

        @Override
        public int maxJitterMillis() {
            return 0;
        }
    }

    @FunctionalInterface
    private interface Invocation {
        LlmFeedbackResult invoke() throws LlmTimeoutException;
    }
}
