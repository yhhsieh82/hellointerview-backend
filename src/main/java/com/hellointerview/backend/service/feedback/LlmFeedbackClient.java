package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.exception.LlmTimeoutException;

public interface LlmFeedbackClient {

    LlmFeedbackResult generate(LlmFeedbackInput input) throws LlmTimeoutException;

    /**
     * Workload identity for Strategy B admission and provider-scoped metrics. All {@link AbstractLlmFeedbackClient}
     * implementations supply a stable key.
     */
    AdmissionWorkloadKey admissionWorkloadKey();
}
