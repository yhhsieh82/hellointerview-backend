package com.hellointerview.backend.service.feedback;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class StubLlmFeedbackClient implements LlmFeedbackClient {

    @Override
    public LlmFeedbackResult generate(LlmFeedbackInput input) {
        String text = "Stub AI feedback for practice " + input.practiceId() + ". "
                + "Diagram summary length: " + input.diagramTextDescription().length() + " characters.";
        return new LlmFeedbackResult(text, 85.5);
    }
}
