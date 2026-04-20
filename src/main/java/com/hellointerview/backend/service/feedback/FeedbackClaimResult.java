package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.dto.FeedbackSubmitResponseDto;

public sealed interface FeedbackClaimResult {

    record Replay(FeedbackSubmitResponseDto dto) implements FeedbackClaimResult {}

    record Proceed(long requestId) implements FeedbackClaimResult {}

    record Conflict(String message) implements FeedbackClaimResult {}

    record InProgress() implements FeedbackClaimResult {}
}
