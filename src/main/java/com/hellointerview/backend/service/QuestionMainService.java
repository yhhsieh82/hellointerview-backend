package com.hellointerview.backend.service;

import com.hellointerview.backend.entity.QuestionMain;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.QuestionMainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuestionMainService {

    private final QuestionMainRepository questionMainRepository;

    public QuestionMainService(QuestionMainRepository questionMainRepository) {
        this.questionMainRepository = questionMainRepository;
    }

    /**
     * Retrieves a QuestionMain by its ID with all associated Questions.
     * Questions are automatically sorted by the 'order' field via @OrderBy annotation on entity.
     *
     * @param id the QuestionMain ID
     * @return the QuestionMain with all questions
     * @throws ResourceNotFoundException if QuestionMain with given ID doesn't exist
     */
    public QuestionMain getQuestionMainById(Long id) {
        return questionMainRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "QuestionMain with id " + id + " does not exist"
                ));
    }
}
