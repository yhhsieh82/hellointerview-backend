package com.hellointerview.backend.service;

import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeMainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class PracticeMainService {

    private static final String DEFAULT_STATUS_PRACTICING = "practicing";

    private final PracticeMainRepository practiceMainRepository;

    public PracticeMainService(PracticeMainRepository practiceMainRepository) {
        this.practiceMainRepository = practiceMainRepository;
    }

    @Transactional(readOnly = true)
    public PracticeMain getActivePracticeMain(Long userId, Long questionMainId, String status) {
        String effectiveStatus = (status == null || status.isBlank()) ? DEFAULT_STATUS_PRACTICING : status;

        return practiceMainRepository
                .findByUserIdAndQuestionMainIdAndStatus(userId, questionMainId, effectiveStatus)
                .orElseThrow(() -> new ResourceNotFoundException("No active practice session found"));
    }

    public PracticeMain createPracticeMain(Long userId, Long questionMainId) {
        PracticeMain practiceMain = PracticeMain.builder()
                .userId(userId)
                .questionMainId(questionMainId)
                .status(DEFAULT_STATUS_PRACTICING)
                .build();

        return practiceMainRepository.save(practiceMain);
    }

    public PracticeMain updatePracticeMainStatus(Long practiceMainId, String status) {
        PracticeMain practiceMain = practiceMainRepository.findById(practiceMainId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PracticeMain with id " + practiceMainId + " does not exist"
                ));

        practiceMain.setStatus(status);
        if ("completed".equals(status)) {
            practiceMain.setCompletedAt(Instant.now());
        } else {
            practiceMain.setCompletedAt(null);
        }

        return practiceMainRepository.save(practiceMain);
    }
}

