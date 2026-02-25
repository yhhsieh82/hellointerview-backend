-- Create practice table for progress tracking (Phase 2: which questions have at least one practice)
CREATE TABLE practice (
    practice_id BIGSERIAL PRIMARY KEY,
    practice_main_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_practice_practice_main
        FOREIGN KEY (practice_main_id)
        REFERENCES practice_main(practice_main_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_practice_question
        FOREIGN KEY (question_id)
        REFERENCES question(question_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_practice_practice_main_id ON practice(practice_main_id);
CREATE INDEX idx_practice_main_question ON practice(practice_main_id, question_id);
