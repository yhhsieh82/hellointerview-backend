package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.Practice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PracticeRepository extends JpaRepository<Practice, Long> {

    @Query("select p from Practice p join fetch p.practiceMain join fetch p.question where p.practiceId = :practiceId")
    Optional<Practice> findWithMainAndQuestionById(@Param("practiceId") Long practiceId);

    @Query("SELECT DISTINCT p.question.questionId FROM Practice p WHERE p.practiceMain.practiceMainId = :practiceMainId")
    List<Long> findDistinctQuestionIdsByPracticeMainId(Long practiceMainId);

    List<Practice> findByPracticeMain_PracticeMainId(Long practiceMainId);

    Optional<Practice> findByPracticeMain_PracticeMainIdAndQuestion_QuestionId(Long practiceMainId, Long questionId);
}
