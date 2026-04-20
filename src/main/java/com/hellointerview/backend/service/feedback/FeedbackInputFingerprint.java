package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.entity.PracticeTranscriptSegment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class FeedbackInputFingerprint {

    private FeedbackInputFingerprint() {
    }

    public static String compute(long practiceId, String sectionJson, List<PracticeTranscriptSegment> segments) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(String.valueOf(practiceId).getBytes(StandardCharsets.UTF_8));
            md.update((byte) '|');
            md.update((sectionJson != null ? sectionJson : "").getBytes(StandardCharsets.UTF_8));
            md.update((byte) '|');
            for (PracticeTranscriptSegment s : segments) {
                md.update((byte) '|');
                md.update(String.valueOf(s.getSegmentOrder()).getBytes(StandardCharsets.UTF_8));
                md.update((byte) '|');
                md.update(String.valueOf(s.getDurationSeconds()).getBytes(StandardCharsets.UTF_8));
                md.update((byte) '|');
                String text = s.getTranscriptText() != null ? s.getTranscriptText() : "";
                md.update(String.valueOf(text.hashCode()).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
