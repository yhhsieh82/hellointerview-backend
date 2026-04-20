package com.hellointerview.backend.service.feedback;

import com.hellointerview.backend.entity.Practice;
import com.hellointerview.backend.entity.PracticeFeedback;
import com.hellointerview.backend.entity.PracticeFeedbackRequest;
import com.hellointerview.backend.entity.PracticeFeedbackRequestStatus;
import com.hellointerview.backend.exception.GradeMappingException;
import com.hellointerview.backend.repository.PracticeFeedbackRepository;
import com.hellointerview.backend.repository.PracticeFeedbackRequestRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class FeedbackIdempotencyCoordinator {

    static final int STALE_CLAIM_AFTER_MINUTES = 15;

    private final PracticeFeedbackRequestRepository requestRepository;
    private final PracticeFeedbackRepository feedbackRepository;

    public FeedbackIdempotencyCoordinator(PracticeFeedbackRequestRepository requestRepository,
                                          PracticeFeedbackRepository feedbackRepository) {
        this.requestRepository = requestRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public FeedbackClaimResult claimOrInsert(Long userId,
                                             String idempotencyKey,
                                             Practice practice,
                                             String fingerprint) {
        Instant now = Instant.now();
        var existingOpt = requestRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existingOpt.isPresent()) {
            return handleExisting(existingOpt.get(), practice, fingerprint, now);
        }
        PracticeFeedbackRequest row = PracticeFeedbackRequest.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .practice(practice)
                .inputFingerprint(fingerprint)
                .status(PracticeFeedbackRequestStatus.CLAIMED)
                .build();
        try {
            requestRepository.saveAndFlush(row);
            return new FeedbackClaimResult.Proceed(row.getPracticeFeedbackRequestId());
        } catch (DataIntegrityViolationException ex) {
            if (!isPostgresUniqueViolation(ex)) {
                throw ex;
            }
            PracticeFeedbackRequest concurrent = requestRepository
                    .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> ex);
            return handleExisting(concurrent, practice, fingerprint, now);
        }
    }

    private FeedbackClaimResult handleExisting(PracticeFeedbackRequest row,
                                               Practice practice,
                                               String fingerprint,
                                               Instant now) {
        if (row.getExpiresAt().isBefore(now)) {
            return new FeedbackClaimResult.Conflict("Idempotency-Key has expired; generate a new key");
        }
        if (!row.getPractice().getPracticeId().equals(practice.getPracticeId())) {
            return new FeedbackClaimResult.Conflict("Idempotency-Key was used for a different practice_id");
        }
        if (row.getStatus() == PracticeFeedbackRequestStatus.CLAIMED && isStaleClaim(row.getCreatedAt(), now)) {
            row.setStatus(PracticeFeedbackRequestStatus.FAILED);
            row.setErrorCode("claim_abandoned");
            row.setPracticeFeedback(null);
            requestRepository.save(row);
            requestRepository.flush();
        }
        return switch (row.getStatus()) {
            case COMPLETED -> handleCompleted(row, fingerprint);
            case CLAIMED -> handleClaimed(row, fingerprint);
            case FAILED -> handleFailed(row, fingerprint);
        };
    }

    private FeedbackClaimResult handleCompleted(PracticeFeedbackRequest row, String fingerprint) {
        if (!fingerprint.equals(row.getInputFingerprint())) {
            return new FeedbackClaimResult.Conflict("Idempotency-Key reused with different persisted inputs");
        }
        PracticeFeedback fb = row.getPracticeFeedback();
        if (fb == null) {
            return new FeedbackClaimResult.Conflict("Completed idempotency row is missing practice_feedback reference");
        }
        return new FeedbackClaimResult.Replay(FeedbackSubmitResponseMapper.toDto(row.getPractice(), fb));
    }

    private FeedbackClaimResult handleClaimed(PracticeFeedbackRequest row, String fingerprint) {
        if (!fingerprint.equals(row.getInputFingerprint())) {
            return new FeedbackClaimResult.Conflict("Idempotency-Key reused with different persisted inputs");
        }
        return new FeedbackClaimResult.InProgress();
    }

    private FeedbackClaimResult handleFailed(PracticeFeedbackRequest row, String fingerprint) {
        if (!fingerprint.equals(row.getInputFingerprint())) {
            return new FeedbackClaimResult.Conflict("Idempotency-Key reused with different persisted inputs");
        }
        row.setStatus(PracticeFeedbackRequestStatus.CLAIMED);
        row.setErrorCode(null);
        row.setPracticeFeedback(null);
        requestRepository.save(row);
        requestRepository.flush();
        return new FeedbackClaimResult.Proceed(row.getPracticeFeedbackRequestId());
    }

    @Transactional
    public PracticeFeedback finalizeSuccessful(long requestId, Practice practice, LlmFeedbackResult result) {
        PracticeFeedbackRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("practice_feedback_request not found: " + requestId));
        double score = clampScoreForPersistence(result.score());
        PracticeFeedback feedback = PracticeFeedback.builder()
                .practice(practice)
                .feedbackText(result.feedbackText())
                .score(score)
                .generatedAt(Instant.now())
                .build();
        feedback = feedbackRepository.save(feedback);
        req.setStatus(PracticeFeedbackRequestStatus.COMPLETED);
        req.setPracticeFeedback(feedback);
        req.setErrorCode(null);
        requestRepository.save(req);
        return feedback;
    }

    @Transactional
    public void markRequestFailed(long requestId, String errorCode) {
        PracticeFeedbackRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("practice_feedback_request not found: " + requestId));
        req.setStatus(PracticeFeedbackRequestStatus.FAILED);
        req.setErrorCode(errorCode);
        req.setPracticeFeedback(null);
        requestRepository.save(req);
    }

    private static boolean isPostgresUniqueViolation(DataIntegrityViolationException ex) {
        for (Throwable c = ex; c != null; c = c.getCause()) {
            if (c instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStaleClaim(Instant createdAt, Instant now) {
        return createdAt.plus(STALE_CLAIM_AFTER_MINUTES, ChronoUnit.MINUTES).isBefore(now);
    }

    /**
     * Finite out-of-range scores are clamped to [0, 100]. NaN/infinite scores cannot be stored or graded (PRD §2.4).
     */
    private static double clampScoreForPersistence(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            throw new GradeMappingException("LLM returned a non-finite score");
        }
        return Math.max(0.0, Math.min(100.0, score));
    }
}
