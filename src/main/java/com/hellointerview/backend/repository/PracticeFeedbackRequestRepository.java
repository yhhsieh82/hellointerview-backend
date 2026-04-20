package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.PracticeFeedbackRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PracticeFeedbackRequestRepository extends JpaRepository<PracticeFeedbackRequest, Long> {

    Optional<PracticeFeedbackRequest> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
