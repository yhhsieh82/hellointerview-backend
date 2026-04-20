package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.PracticeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PracticeFeedbackRepository extends JpaRepository<PracticeFeedback, Long> {

    List<PracticeFeedback> findByPractice_PracticeIdIn(Collection<Long> practiceIds);

    @Query("select distinct pf.practice.question.questionId from PracticeFeedback pf "
            + "where pf.practice.practiceMain.practiceMainId = :practiceMainId")
    List<Long> findDistinctQuestionIdsWithFeedbackByPracticeMainId(@Param("practiceMainId") Long practiceMainId);
}
