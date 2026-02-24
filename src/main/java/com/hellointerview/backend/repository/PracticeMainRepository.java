package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.PracticeMain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PracticeMainRepository extends JpaRepository<PracticeMain, Long> {

    Optional<PracticeMain> findByUserIdAndQuestionMainIdAndStatus(Long userId, Long questionMainId, String status);
}

