package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.dto.FeedbackGradeView;
import com.hellointerview.backend.exception.GradeMappingException;

public final class FeedbackGradeMapper {

    private FeedbackGradeMapper() {
    }

    public static FeedbackGradeView fromScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            throw new GradeMappingException("Invalid score for grade mapping");
        }
        if (score < 0 || score > 100) {
            throw new GradeMappingException("Score must be between 0 and 100");
        }
        int s = (int) Math.floor(score);
        if (s <= 19) {
            return new FeedbackGradeView("Needs Improvement", "score_needs_improvement_red");
        }
        if (s <= 39) {
            return new FeedbackGradeView("Below Expectations", "score_below_expectations_orange");
        }
        if (s <= 59) {
            return new FeedbackGradeView("Developing", "score_developing_yellow");
        }
        if (s <= 79) {
            return new FeedbackGradeView("Good", "score_good_blue");
        }
        return new FeedbackGradeView("Strong", "score_strong_green");
    }
}
