package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.PracticeMainResponseDto;
import com.hellointerview.backend.entity.PracticeMain;
import com.hellointerview.backend.exception.ResourceNotFoundException;
import com.hellointerview.backend.repository.PracticeMainRepository;
import com.hellointerview.backend.repository.PracticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class PracticeMainService {

    private static final String DEFAULT_STATUS_PRACTICING = "practicing";

    private final PracticeMainRepository practiceMainRepository;
    private final PracticeRepository practiceRepository;

    public PracticeMainService(PracticeMainRepository practiceMainRepository,
                               PracticeRepository practiceRepository) {
        this.practiceMainRepository = practiceMainRepository;
        this.practiceRepository = practiceRepository;
    }

    @Transactional(readOnly = true)
    public List<Long> getQuestionIdsWithPractices(Long practiceMainId) {
        return practiceRepository.findDistinctQuestionIdsByPracticeMainId(practiceMainId);
    }

    @Transactional(readOnly = true)
    public PracticeMainResponseDto getActivePracticeMainWithProgress(Long userId, Long questionMainId, String status) {
        PracticeMain practiceMain = getActivePracticeMain(userId, questionMainId, status);
        List<Long> questionIdsWithPractices = getQuestionIdsWithPractices(practiceMain.getPracticeMainId());
        return toResponseDto(practiceMain, questionIdsWithPractices);
    }

    private static PracticeMainResponseDto toResponseDto(PracticeMain practiceMain, List<Long> questionIdsWithPractices) {
        return new PracticeMainResponseDto(
                practiceMain.getPracticeMainId(),
                practiceMain.getUserId(),
                practiceMain.getQuestionMainId(),
                practiceMain.getStatus(),
                practiceMain.getStartedAt(),
                practiceMain.getCompletedAt(),
                questionIdsWithPractices
        );
    }

    @Transactional(readOnly = true)
    public PracticeMain getActivePracticeMain(Long userId, Long questionMainId, String status) {
        String effectiveStatus = (status == null || status.isBlank()) ? DEFAULT_STATUS_PRACTICING : status;

        return practiceMainRepository
                .findByUserIdAndQuestionMainIdAndStatus(userId, questionMainId, effectiveStatus)
                .orElseThrow(() -> new ResourceNotFoundException("No active practice session found"));
    }

    public PracticeMain createPracticeMain(Long userId, Long questionMainId) {
        PracticeMain practiceMain = new PracticeMain();
        practiceMain.setUserId(userId);
        practiceMain.setQuestionMainId(questionMainId);
        practiceMain.setStatus(DEFAULT_STATUS_PRACTICING);

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

