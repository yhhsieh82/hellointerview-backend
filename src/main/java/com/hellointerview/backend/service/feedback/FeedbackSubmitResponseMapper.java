package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.dto.FeedbackGradeView;
import com.hellointerview.backend.dto.FeedbackPayloadDto;
import com.hellointerview.backend.dto.FeedbackSubmitResponseDto;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeFeedback;

import java.time.Instant;

public final class FeedbackSubmitResponseMapper {

    private FeedbackSubmitResponseMapper() {
    }

    public static FeedbackSubmitResponseDto toDto(Practice practice, PracticeFeedback feedback) {
        FeedbackGradeView grade = FeedbackGradeMapper.fromScore(feedback.getScore());
        FeedbackPayloadDto payload = new FeedbackPayloadDto(
                feedback.getPracticeFeedbackId(),
                feedback.getFeedbackText(),
                feedback.getScore(),
                grade.gradeLabel(),
                grade.gradeColor(),
                feedback.getGeneratedAt()
        );
        return new FeedbackSubmitResponseDto(
                practice.getPracticeId(),
                payload,
                feedback.getGeneratedAt()
        );
    }
}
