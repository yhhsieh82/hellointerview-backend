-- Create transcript segment tables for audio recording (active + history)

CREATE TABLE practice_transcript_segment (
    segment_id BIGSERIAL PRIMARY KEY,
    practice_id BIGINT NOT NULL,
    segment_order INTEGER NOT NULL,
    transcript_text TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_practice_transcript_segment_practice
        FOREIGN KEY (practice_id)
        REFERENCES practice(practice_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_practice_transcript_segment_practice_order
        UNIQUE (practice_id, segment_order),
    CONSTRAINT chk_practice_transcript_segment_duration_positive
        CHECK (duration_seconds > 0)
);

CREATE INDEX idx_practice_transcript_segment_practice_id
    ON practice_transcript_segment(practice_id);

CREATE INDEX idx_practice_transcript_segment_practice_order
    ON practice_transcript_segment(practice_id, segment_order);

CREATE TABLE practice_transcript_segment_history (
    segment_id BIGINT PRIMARY KEY,
    practice_id BIGINT NOT NULL,
    segment_order INTEGER NOT NULL,
    transcript_text TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_practice_transcript_segment_history_practice
        FOREIGN KEY (practice_id)
        REFERENCES practice_history(practice_id),
    CONSTRAINT uq_practice_transcript_segment_history_practice_order
        UNIQUE (practice_id, segment_order),
    CONSTRAINT chk_practice_transcript_segment_history_duration_positive
        CHECK (duration_seconds > 0)
);

CREATE INDEX idx_practice_transcript_segment_history_practice_id
    ON practice_transcript_segment_history(practice_id);
