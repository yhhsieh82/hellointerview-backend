package com.hellointerview.backend.service.feedback;

import java.time.Duration;

interface LlmRetryPolicyProperties {

    int maxAttempts();

    Duration initialBackoff();

    double backoffMultiplier();

    int maxJitterMillis();
}
