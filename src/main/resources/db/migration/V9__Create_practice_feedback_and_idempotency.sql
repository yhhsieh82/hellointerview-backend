-- Active AI feedback rows and idempotent submit tracking (see AI Feedback PRD §4.1)

CREATE TABLE practice_feedback (
    practice_feedback_id BIGSERIAL PRIMARY KEY,
    practice_id BIGINT NOT NULL,
    feedback_text TEXT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_practice_feedback_practice
        FOREIGN KEY (practice_id)
        REFERENCES practice(practice_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_practice_feedback_practice_id ON practice_feedback(practice_id);

CREATE TABLE practice_feedback_request (
    practice_feedback_request_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    practice_id BIGINT NOT NULL,
    input_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    practice_feedback_id BIGINT,
    error_code VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_pfr_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pfr_practice
        FOREIGN KEY (practice_id)
        REFERENCES practice(practice_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_pfr_practice_feedback
        FOREIGN KEY (practice_feedback_id)
        REFERENCES practice_feedback(practice_feedback_id)
        ON DELETE SET NULL,
    CONSTRAINT uq_pfr_user_idempotency_key UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_pfr_practice_status ON practice_feedback_request(practice_id, status);
