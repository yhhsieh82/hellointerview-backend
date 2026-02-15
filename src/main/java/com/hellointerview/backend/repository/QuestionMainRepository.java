package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.QuestionMain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionMainRepository extends JpaRepository<QuestionMain, Long> {

    /**
     * Find QuestionMain by ID with all associated Questions eagerly fetched
     * to avoid N+1 query problem
     */
    @Query("SELECT qm FROM QuestionMain qm LEFT JOIN FETCH qm.questions WHERE qm.questionMainId = :id")
    Optional<QuestionMain> findByIdWithQuestions(@Param("id") Long id);
}
