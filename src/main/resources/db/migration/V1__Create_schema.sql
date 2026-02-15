-- Create question_main table
CREATE TABLE question_main (
    question_main_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    write_up TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on name for searching
CREATE INDEX idx_question_main_name ON question_main(name);

-- Create question table
CREATE TABLE question (
    question_id BIGSERIAL PRIMARY KEY,
    question_main_id BIGINT NOT NULL,
    "order" INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    whiteboard_section INTEGER NOT NULL CHECK (whiteboard_section BETWEEN 1 AND 5),
    requires_recording BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_question_main
        FOREIGN KEY (question_main_id)
        REFERENCES question_main(question_main_id)
        ON DELETE CASCADE
);

-- Create indexes for efficient querying
CREATE INDEX idx_question_main_id ON question(question_main_id);
CREATE INDEX idx_question_order ON question(question_main_id, "order");

-- Add constraint to ensure valid question types
ALTER TABLE question ADD CONSTRAINT chk_question_type 
    CHECK (type IN ('FUNCTIONAL_REQ', 'NON_FUNCTIONAL_REQ', 'ENTITIES', 'API', 'HIGH_LEVEL_DESIGN', 'DEEP_DIVE'));

-- Function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update updated_at on question_main
CREATE TRIGGER update_question_main_updated_at
    BEFORE UPDATE ON question_main
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
