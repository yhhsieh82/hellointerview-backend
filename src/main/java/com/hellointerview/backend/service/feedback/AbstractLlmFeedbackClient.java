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
    private final LlmProviderMetrics metrics;

    protected AbstractLlmFeedbackClient(FeedbackPromptTemplate promptTemplate,
                                        LlmRetryPolicyProperties retryPolicyProperties,
                                        Logger logger,
                                        LlmProviderMetrics metrics) {
        this.promptTemplate = promptTemplate;
        this.retryPolicyProperties = retryPolicyProperties;
        this.logger = logger;
        this.metrics = metrics;
    }

    @Override
    public AdmissionWorkloadKey admissionWorkloadKey() {
        return new AdmissionWorkloadKey(providerName(), modelName());
    }

    @Override
    public LlmFeedbackResult generate(LlmFeedbackInput input) throws LlmTimeoutException {
        String prompt = promptTemplate.render(input);
        String provider = providerName();
        String model = modelName();
        int attempts = retryPolicyProperties.maxAttempts();
        Duration nextBackoff = retryPolicyProperties.initialBackoff();
        RuntimeException lastTransientFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            long attemptStartNanos = System.nanoTime();
            metrics.incrementInflight(provider, model);
            try {
                LlmFeedbackResult result = invokeProvider(prompt);
                metrics.recordCall(provider, model, attempt, "success", durationSince(attemptStartNanos));
                metrics.recordCallsPerSuccess(provider, model, attempt);
                if (attempt > 1) {
                    metrics.recordRetryOutcome(provider, model, "success_after_retry");
                }
                return result;
            } catch (LlmTimeoutException e) {
                metrics.recordCall(provider, model, attempt, "failure", durationSince(attemptStartNanos));
                metrics.recordFailureClass(provider, model, "provider_timeout");
                if (attempt == attempts) {
                    if (attempts > 1) {
                        metrics.recordRetryOutcome(provider, model, "exhausted");
                    }
                    throw e;
                }
                metrics.recordRetryAttempt(provider, model, "timeout");
                sleepQuietly(nextBackoff.plusMillis(randomJitterMillis()));
                nextBackoff = multiplyBackoff(nextBackoff);
                lastTransientFailure = e;
            } catch (ResourceAccessException e) {
                metrics.recordCall(provider, model, attempt, "failure", durationSince(attemptStartNanos));
                if (!isTimeout(e)) {
                    metrics.recordFailureClass(provider, model, "unknown");
                    throw new LlmProviderException(providerName() + " network call failed", true, e);
                }
                metrics.recordFailureClass(provider, model, "provider_timeout");
                if (attempt == attempts) {
                    if (attempts > 1) {
                        metrics.recordRetryOutcome(provider, model, "exhausted");
                    }
                    throw new LlmTimeoutException("Timed out waiting for " + providerName() + " response");
                }
                metrics.recordRetryAttempt(provider, model, "timeout");
                sleepQuietly(nextBackoff.plusMillis(randomJitterMillis()));
                nextBackoff = multiplyBackoff(nextBackoff);
                lastTransientFailure = e;
            } catch (RestClientResponseException e) {
                metrics.recordCall(provider, model, attempt, "failure", durationSince(attemptStartNanos));
                HttpStatusCode statusCode = e.getStatusCode();
                metrics.recordHttpStatus(provider, model, statusCode.value());
                String failureClass = LlmProviderMetrics.classifyHttpStatus(statusCode.value());
                metrics.recordFailureClass(provider, model, failureClass);
                if (isTransientHttp(statusCode.value())) {
                    if (attempt == attempts) {
                        if (attempts > 1) {
                            metrics.recordRetryOutcome(provider, model, "exhausted");
                        }
                        throw new LlmProviderException(providerName() + " transient failure after retries", true, e);
                    }
                    Duration retryDelay = parseRetryAfter(e);
                    if (retryDelay != null) {
                        metrics.recordRetryAttempt(provider, model, statusCode.value() == 429 ? "429" : "5xx");
                        metrics.recordRetryAfterSeconds(provider, model, retryDelay);
                        sleepQuietly(retryDelay);
                    } else {
                        metrics.recordRetryAttempt(provider, model, statusCode.value() == 429 ? "429" : "5xx");
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
            } catch (LlmProviderException e) {
                metrics.recordCall(provider, model, attempt, "failure", durationSince(attemptStartNanos));
                metrics.recordFailureClass(provider, model, LlmProviderMetrics.classifyProviderException(e));
                throw e;
            } finally {
                metrics.decrementInflight(provider, model);
            }
        }

        if (lastTransientFailure != null) {
            if (attempts > 1) {
                metrics.recordRetryOutcome(provider, model, "exhausted");
            }
            throw new LlmProviderException(providerName() + " transient failure after retries", true, lastTransientFailure);
        }
        throw new LlmProviderException(providerName() + " call failed unexpectedly", true);
    }

    protected abstract String providerName();

    protected abstract String modelName();

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

    private Duration durationSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
