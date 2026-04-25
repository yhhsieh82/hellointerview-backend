package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.exception.LlmTimeoutException;
import org.slf4j.Logger;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

abstract class AbstractLlmFeedbackClient implements LlmFeedbackClient {

    private final FeedbackPromptTemplate promptTemplate;
    private final LlmRetryPolicyProperties retryPolicyProperties;
    private final Logger logger;

    protected AbstractLlmFeedbackClient(FeedbackPromptTemplate promptTemplate,
                                        LlmRetryPolicyProperties retryPolicyProperties,
                                        Logger logger) {
        this.promptTemplate = promptTemplate;
        this.retryPolicyProperties = retryPolicyProperties;
        this.logger = logger;
    }

    @Override
    public LlmFeedbackResult generate(LlmFeedbackInput input) throws LlmTimeoutException {
        String prompt = promptTemplate.render(input);
        int attempts = retryPolicyProperties.maxAttempts();
        Duration nextBackoff = retryPolicyProperties.initialBackoff();
        RuntimeException lastTransientFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return invokeProvider(prompt);
            } catch (LlmTimeoutException e) {
                if (attempt == attempts) {
                    throw e;
                }
                sleepQuietly(nextBackoff.plusMillis(randomJitterMillis()));
                nextBackoff = multiplyBackoff(nextBackoff);
                lastTransientFailure = e;
            } catch (ResourceAccessException e) {
                if (!isTimeout(e)) {
                    throw new LlmProviderException(providerName() + " network call failed", true, e);
                }
                if (attempt == attempts) {
                    throw new LlmTimeoutException("Timed out waiting for " + providerName() + " response");
                }
                sleepQuietly(nextBackoff.plusMillis(randomJitterMillis()));
                nextBackoff = multiplyBackoff(nextBackoff);
                lastTransientFailure = e;
            } catch (RestClientResponseException e) {
                HttpStatusCode statusCode = e.getStatusCode();
                if (isTransientHttp(statusCode.value())) {
                    if (attempt == attempts) {
                        throw new LlmProviderException(providerName() + " transient failure after retries", true, e);
                    }
                    Duration retryDelay = parseRetryAfter(e);
                    if (retryDelay != null) {
                        sleepQuietly(retryDelay);
                    } else {
                        sleepQuietly(nextBackoff.plusMillis(randomJitterMillis()));
                        nextBackoff = multiplyBackoff(nextBackoff);
                    }
                    lastTransientFailure = e;
                    continue;
                }
                throw new LlmProviderException(
                        providerName() + " terminal failure status " + statusCode.value(),
                        false,
                        e
                );
            }
        }

        if (lastTransientFailure != null) {
            throw new LlmProviderException(providerName() + " transient failure after retries", true, lastTransientFailure);
        }
        throw new LlmProviderException(providerName() + " call failed unexpectedly", true);
    }

    protected abstract String providerName();

    protected abstract LlmFeedbackResult invokeProvider(String prompt) throws LlmTimeoutException;

    protected Duration parseRetryAfter(RestClientResponseException e) {
        return null;
    }

    protected boolean isTransientHttp(int status) {
        return status == 429 || status >= 500;
    }

    protected static boolean isTimeout(ResourceAccessException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private Duration multiplyBackoff(Duration current) {
        long millis = Math.round(current.toMillis() * retryPolicyProperties.backoffMultiplier());
        return Duration.ofMillis(Math.max(1, millis));
    }

    private int randomJitterMillis() {
        int max = retryPolicyProperties.maxJitterMillis();
        if (max <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(max + 1);
    }

    private void sleepQuietly(Duration duration) {
        if (duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting to retry {} call", providerName());
        }
    }
}
