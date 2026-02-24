-- Create app_user table (Phase 1 User implementation)
CREATE TABLE app_user (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Trigger to auto-update updated_at on app_user using existing function
CREATE TRIGGER update_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Seed at least one test user for development and tests
INSERT INTO app_user (email, name, created_at, updated_at)
VALUES ('test.user@example.com', 'Test User', NOW(), NOW());

-- Create practice_main table for practice session lifecycle
CREATE TABLE practice_main (
    practice_main_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_main_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'practicing',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_practice_main_user
        FOREIGN KEY (user_id)
        REFERENCES app_user(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_practice_main_question_main
        FOREIGN KEY (question_main_id)
        REFERENCES question_main(question_main_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_practice_main_status
        CHECK (status IN ('practicing', 'completed'))
);

-- Index to efficiently find active sessions by user/question/status
CREATE INDEX idx_practice_main_user_question_status
    ON practice_main(user_id, question_main_id, status);

