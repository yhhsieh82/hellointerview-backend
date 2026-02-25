package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.Practice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PracticeRepository extends JpaRepository<Practice, Long> {

    @Query("SELECT DISTINCT p.question.questionId FROM Practice p WHERE p.practiceMain.practiceMainId = :practiceMainId")
    List<Long> findDistinctQuestionIdsByPracticeMainId(Long practiceMainId);

    List<Practice> findByPracticeMain_PracticeMainId(Long practiceMainId);
}
