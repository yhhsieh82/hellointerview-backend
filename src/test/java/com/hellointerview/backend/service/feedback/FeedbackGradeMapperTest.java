package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.exception.GradeMappingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeedbackGradeMapperTest {

    @Test
    void fromScore_mapsBandBoundaries() {
        assertEquals("Needs Improvement", FeedbackGradeMapper.fromScore(0).gradeLabel());
        assertEquals("Below Expectations", FeedbackGradeMapper.fromScore(20).gradeLabel());
        assertEquals("Developing", FeedbackGradeMapper.fromScore(40).gradeLabel());
        assertEquals("Good", FeedbackGradeMapper.fromScore(60).gradeLabel());
        assertEquals("Strong", FeedbackGradeMapper.fromScore(80).gradeLabel());
        assertEquals("Strong", FeedbackGradeMapper.fromScore(100).gradeLabel());
    }

    @Test
    void fromScore_nanThrowsGradeMappingException() {
        assertThrows(GradeMappingException.class, () -> FeedbackGradeMapper.fromScore(Double.NaN));
    }

    @Test
    void fromScore_outOfRangeThrowsGradeMappingException() {
        assertThrows(GradeMappingException.class, () -> FeedbackGradeMapper.fromScore(-0.1));
        assertThrows(GradeMappingException.class, () -> FeedbackGradeMapper.fromScore(100.1));
    }
}
