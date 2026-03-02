package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.QuestionMainSummaryDto;
import com.hellointerview.backend.entity.QuestionMain;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.QuestionMainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
@Transactional(readOnly = true)
public class QuestionMainService {

    private final QuestionMainRepository questionMainRepository;

    public QuestionMainService(QuestionMainRepository questionMainRepository) {
        this.questionMainRepository = questionMainRepository;
    }

    /**
     * Returns all QuestionMains as summary DTOs for listing (e.g. problem list).
     * Does not include write_up or questions to keep the response light.
     */
    public List<QuestionMainSummaryDto> getAllQuestionMains() {
        return StreamSupport.stream(questionMainRepository.findAll().spliterator(), false)
                .map(this::toSummaryDto)
                .toList();
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

    private QuestionMainSummaryDto toSummaryDto(QuestionMain qm) {
        return new QuestionMainSummaryDto(
                qm.getQuestionMainId(),
                qm.getName(),
                qm.getDescription(),
                qm.getCreatedAt(),
                qm.getUpdatedAt()
        );
    }
}
