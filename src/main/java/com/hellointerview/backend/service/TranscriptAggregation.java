package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.PracticeQuestionStateDto;
import com.hellointerview.backend.dto.TranscriptSegmentDto;
import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeTranscriptSegment;

import java.util.List;

final class TranscriptAggregation {

    private TranscriptAggregation() {
    }

    static String buildCombinedTranscript(List<PracticeTranscriptSegment> orderedSegments) {
        return orderedSegments.stream()
                .map(PracticeTranscriptSegment::getTranscriptText)
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    static List<TranscriptSegmentDto> toSegmentDtos(List<PracticeTranscriptSegment> orderedSegments) {
        return orderedSegments.stream()
                .map(segment -> new TranscriptSegmentDto(
                        segment.getSegmentOrder(),
                        segment.getTranscriptText(),
                        segment.getDurationSeconds()
                ))
                .toList();
    }

    static int totalDurationSeconds(List<PracticeTranscriptSegment> orderedSegments) {
        return orderedSegments.stream()
                .mapToInt(PracticeTranscriptSegment::getDurationSeconds)
                .sum();
    }

    static PracticeQuestionStateDto toPracticeQuestionStateDto(Practice practice, List<PracticeTranscriptSegment> orderedSegments) {
        return new PracticeQuestionStateDto(
                practice.getPracticeId(),
                practice.getPracticeMain().getPracticeMainId(),
                practice.getQuestion().getQuestionId(),
                toSegmentDtos(orderedSegments),
                totalDurationSeconds(orderedSegments),
                buildCombinedTranscript(orderedSegments)
        );
    }
}
