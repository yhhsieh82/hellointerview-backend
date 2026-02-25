-- Create history tables for completed practice sessions (Phase 3: completion semantics and history)

CREATE TABLE practice_main_history (
    practice_main_id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_main_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_practice_main_history_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(user_id),
    CONSTRAINT fk_practice_main_history_question_main
        FOREIGN KEY (question_main_id)
        REFERENCES question_main(question_main_id)
);

CREATE INDEX idx_practice_main_history_user_question
    ON practice_main_history(user_id, question_main_id);

CREATE TABLE practice_history (
    practice_id BIGINT PRIMARY KEY,
    practice_main_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_practice_history_practice_main_history
        FOREIGN KEY (practice_main_id)
        REFERENCES practice_main_history(practice_main_id),
    CONSTRAINT fk_practice_history_question
        FOREIGN KEY (question_id)
        REFERENCES question(question_id)
);

CREATE INDEX idx_practice_history_practice_main_id
    ON practice_history(practice_main_id);

CREATE TABLE practice_feedback_history (
    practice_feedback_id BIGSERIAL PRIMARY KEY,
    practice_id BIGINT NOT NULL,
    feedback_text TEXT NOT NULL,
    score DOUBLE PRECISION,
    generated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_practice_feedback_history_practice
        FOREIGN KEY (practice_id)
        REFERENCES practice_history(practice_id)
);

