package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.PracticeHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeHistoryRepository extends JpaRepository<PracticeHistory, Long> {
}

