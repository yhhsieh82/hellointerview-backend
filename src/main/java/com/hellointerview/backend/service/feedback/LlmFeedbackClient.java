package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.exception.LlmTimeoutException;

public interface LlmFeedbackClient {

    LlmFeedbackResult generate(LlmFeedbackInput input) throws LlmTimeoutException;
}
