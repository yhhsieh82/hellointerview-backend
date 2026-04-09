package com.hellointerview.backend.repository;

import com.hellointerview.backend.entity.PracticeTranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PracticeTranscriptSegmentRepository extends JpaRepository<PracticeTranscriptSegment, Long> {

    List<PracticeTranscriptSegment> findByPractice_PracticeIdOrderBySegmentOrderAsc(Long practiceId);
    List<PracticeTranscriptSegment> findByPractice_PracticeIdIn(List<Long> practiceIds);

    @Query("SELECT COALESCE(MAX(pts.segmentOrder), 0) FROM PracticeTranscriptSegment pts WHERE pts.practice.practiceId = :practiceId")
    Integer findMaxSegmentOrderByPracticeId(Long practiceId);

    @Query("SELECT COALESCE(SUM(pts.durationSeconds), 0) FROM PracticeTranscriptSegment pts WHERE pts.practice.practiceId = :practiceId")
    Integer findTotalDurationSecondsByPracticeId(Long practiceId);
}
