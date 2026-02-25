package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.PracticeFeedbackHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeFeedbackHistoryRepository extends JpaRepository<PracticeFeedbackHistory, Long> {
}

