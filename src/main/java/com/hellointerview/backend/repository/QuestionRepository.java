package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Find all questions for a given QuestionMain, ordered by the 'order' field
     */
    List<Question> findByQuestionMainIdOrderByOrder(Long questionMainId);
}
