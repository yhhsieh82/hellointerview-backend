package com.hellointerview.backend.service;

import com.hellointerview.backend.dto.QuestionMainSummaryDto;
import com.hellointerview.backend.entity.QuestionMain;
import com.hellointerview.backend.repository.QuestionMainRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionMainServiceListTest {

    @Mock
    private QuestionMainRepository questionMainRepository;

    @InjectMocks
    private QuestionMainService questionMainService;

    private static final Instant CREATED_AT = Instant.parse("2026-02-13T09:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-14T10:30:00Z");

    @Test
    void getAllQuestionMains_MapsEntitiesToSummaryDtoCorrectly() {
        List<QuestionMain> entities = List.of(
                QuestionMain.builder()
                        .questionMainId(1L)
                        .name("Design Twitter")
                        .description("Design a social media platform.")
                        .writeUp("# Sample answer...")
                        .createdAt(CREATED_AT)
                        .updatedAt(UPDATED_AT)
                        .build(),
                QuestionMain.builder()
                        .questionMainId(2L)
                        .name("Design URL Shortener")
                        .description("Shorten and redirect URLs.")
                        .writeUp("# Answer...")
                        .createdAt(CREATED_AT)
                        .updatedAt(UPDATED_AT)
                        .build()
        );
        when(questionMainRepository.findAll()).thenReturn(entities);

        List<QuestionMainSummaryDto> result = questionMainService.getAllQuestionMains();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getQuestionMainId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Design Twitter");
        assertThat(result.get(0).getDescription()).isEqualTo("Design a social media platform.");
        assertThat(result.get(0).getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(result.get(0).getUpdatedAt()).isEqualTo(UPDATED_AT);
        assertThat(result.get(1).getQuestionMainId()).isEqualTo(2L);
        assertThat(result.get(1).getName()).isEqualTo("Design URL Shortener");
        assertThat(result.get(1).getDescription()).isEqualTo("Shorten and redirect URLs.");
        verify(questionMainRepository, times(1)).findAll();
    }

    @Test
    void getAllQuestionMains_WhenNoEntities_ReturnsEmptyList() {
        when(questionMainRepository.findAll()).thenReturn(List.of());

        List<QuestionMainSummaryDto> result = questionMainService.getAllQuestionMains();

        assertThat(result).isEmpty();
        verify(questionMainRepository, times(1)).findAll();
    }
}
